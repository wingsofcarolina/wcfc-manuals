<script>
	import { onMount } from 'svelte'
	import { goto } from '@sapper/app'
	import { user, adminState } from '../store.js'
	import { getUser } from '../common.js'
	import { Circle2 } from 'svelte-loading-spinners'
	import Checkmark from "../components/Checkmark.svelte";
	import UploadDialog from "../components/UploadDialog.svelte";

	let equipment = null;
	let equipmentTypes = null;
	let aircraft = null;
	let aircraftTypes = null;

	let dialog;
	let data = null;

	onMount(async () => {
	  getUser();
		getAircraftTypes();
		getAircraft();
		getEquipmentTypes();
	  getEquipment();
	});

	const getAircraft = async () => {
	  const response = await fetch('/api/aircraft', {
	    method: "get",
	    withCredentials: true,
	    headers: {
	      'Accept': 'application/json'
	    }
	  });
	  if (!response.ok) {
			console.log('Status : ', status);
	    if (response.status == 401) {
	      console.log('User not authenticated, redirecting to Slack');
	      goto('login');
	    } else {
	      notifier.danger('Retrieve of aircraft list failed.');
	    }
	  } else {
	    aircraft = await response.json();
	  }
	}

	const getAircraftTypes= async () => {
	  const response = await fetch('/api/aircraft/types', {
	    method: "get",
	    withCredentials: true,
	    headers: {
	      'Accept': 'application/json'
	    }
	  });
	  if (!response.ok) {
	    if (response.status == 401) {
	      console.log('User not authenticated, redirecting to Slack');
	      goto('login');
	    } else {
	      notifier.danger('Retrieve of aircraft list failed.');
	    }
	  } else {
	    aircraftTypes = await response.json();
	  }
	}

	const getEquipment = async () => {
	  const response = await fetch('/api/equipment', {
	    method: "get",
	    withCredentials: true,
	    headers: {
	      'Accept': 'application/json'
	    }
	  });
	  if (!response.ok) {
	    if (response.status == 401) {
	      console.log('User not authenticated, redirecting to Slack');
	      goto('login');
	    } else {
	      notifier.danger('Retrieve of equipment list failed.');
	    }
	  } else {
	    equipment = await response.json();
	  }
	}

	const getEquipmentTypes = async () => {
	  const response = await fetch('/api/equipment/types', {
	    method: "get",
	    withCredentials: true,
	    headers: {
	      'Accept': 'application/json'
	    }
	  });
	  if (!response.ok) {
	    if (response.status == 401) {
	      console.log('User not authenticated, redirecting to Slack');
	      goto('login');
	    } else {
	      notifier.danger('Retrieve of equipment type list failed.');
	    }
	  } else {
	    equipmentTypes = await response.json();
	  }
	}

	const sleep = (milliseconds) => {
	  return new Promise(resolve => setTimeout(resolve, milliseconds))
	}

	const fetchFile = async (uuid) => {
		document.body.style.cursor='wait';
		document.getElementById('wait').style.visibility = 'visible';

    const response = await fetch('/api/fetch/' + uuid, {
      method: "get",
      withCredentials: true,
      headers: {
        'Accept': 'application/pdf',
        'Content-Type': 'application/json'
      },
    });
    if (!response.ok) {
      notifier.danger('Retrieve of requested document failed.');
    } else {
      data = await response.blob();
      var fileURL = URL.createObjectURL(data);
      window.open(fileURL, 'wcfc-manuals');

			document.body.style.cursor='default';
			document.getElementById('wait').style.visibility = 'hidden';
    }
  }

	const modify = (event) => {
	  var entity = event.detail.entity;
	  if (entity != null) {
	    var item = entity.item;
	  }
	  var command = event.detail.command;

	  if (command == 'delete') {
	    deleteItem(item);
	  } else if (command == 'moveUp') {
	    move('moveUp', item);
	  } else if (command == 'moveDown') {
	    move('moveDown', item);
	  }
	  sleep(100).then(() => {
	    getEquipment();
	  })
	}

	function upload(name, uuid) {
		dialog.raise(name, uuid);
	}

	function refresh() {
		sleep(50).then(() => {
			getAircraft();
			getEquipment();
		})
	}
</script>

<svelte:head>
	<title>WCFC Flight Equipment</title>
</svelte:head>

	{#if equipment && equipmentTypes && aircraft && aircraftTypes}
		<table id="equipment">
			<tr>
				<th class=blank>&nbsp;</th>
				{#each aircraftTypes as type }
					<th class='type' colspan={type.count}>
						<span class='label'>{type.label}</span>
					</th>
				{/each}
			</tr>

			<tr>
				<th>Equipment</th>
				{#each aircraft as {registration, uuid, hasDocument } }
					<th class=reg>
						{#if $user &&  $adminState == 'on' && ! $user.anonymous}
							<span class='link admin' on:click={upload(registration, uuid)}>
								{registration}
								{#if hasDocument}
									<img src='document_white.png' alt='Document'>
								{/if}
							</span>
						{:else}
							{#if hasDocument}
								<span class='link' on:click={fetchFile(uuid)}>
									{registration} <img src='document_white.png' alt='Document'>
								</span>
							{:else}
								<span class='label'>{registration}</span>
							{/if}
						{/if}
					</th>
				{/each}
			</tr>

			{#key aircraft}
				{#each equipmentTypes as {mtype, label} }
					<tr><td><span class='label'>{label}</span></td></tr>
					{#each equipment as { name, type, uuid, hasDocument } }
						{#if mtype === type}
							<tr class='detail'>
								{#if $user &&  $adminState == 'on' && ! $user.anonymous}
									<td><span class='equipment link admin' on:click={upload(name, uuid)}>
										{name}
										{#if hasDocument}
											<img src='document.png' alt='Document'>
										{/if}
									</span></td>
								{:else}
									{#if hasDocument}
										<td><span class='equipment link' on:click={fetchFile(uuid)}>
											{name}<img src='document.png' alt='Document'>
										</span></td>
									{:else}
										<td><span class='equipment'>{name}</span></td>
									{/if}
								{/if}
								{#each aircraft as acft }
									<Checkmark uuid={uuid} aircraft={acft} on:message={refresh}/>
								{/each}
							</tr>
						{/if}
					{/each}
				{/each}
			{/key}
		</table>
	{/if}

	<UploadDialog bind:this="{dialog}" on:modify on:message={refresh}/>

	<div id='wait'>
		<Circle2 size="100" color="#7887a2" unit="px"></Circle2>
	</div>

<style>
#wait {
	visibility: hidden;
	position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.detail img {
	margin-left: 5px;
}
#equipment .equipment { text-align: right; float: right; }
#equipment .admin { }
#equipment .link { cursor: pointer; }
#equipment .detail { text-align: center; }
#equipment .detail:hover {background-color: #ddd;}
#equipment .blank { background-color:rgba(0, 0, 0, 0); width: 30%; }
#equipment .label { font-weight: bold; font-size: 1.2em }
#equipment .reg {
	width: 30px;
	-webkit-writing-mode: vertical-lr;
	writing-mode: vertical-lr;
	-webkit-text-orientation: sideways;
	text-orientation: sideways;
	text-align: end;
	padding: 12px;
}
#equipment th {
  padding-top: 5px;
  padding-bottom: 5px;
  background-color: #7887a2;
  color: white;
}

@media (min-width: 480px) {
}
</style>
