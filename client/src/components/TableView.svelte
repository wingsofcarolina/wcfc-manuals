<script>
	import { onMount } from 'svelte'
	import { goto } from '$app/navigation'
	import { user, adminState } from '../store.js'
	import { getUser } from '../common.js'
	import { Circle2 } from 'svelte-loading-spinners'
	import { notifier } from '@beyonk/svelte-notifications'
	import Checkmark from "../components/Checkmark.svelte";
	import UploadDialog from "../components/UploadDialog.svelte";
	import NewEquipmentDialog from "../components/NewEquipmentDialog.svelte";
	import DeleteEquipmentDialog from "../components/DeleteEquipmentDialog.svelte";
	import NewAircraftDialog from "../components/NewAircraftDialog.svelte";

	let equipment = null;
	let equipmentTypes = null;
	let aircraft = null;
	let aircraftTypes = null;

	let upload_dialog;
	let equipment_dialog;
	let delete_dialog;
	let aircraft_dialog;
	let data = null;

	let archive_details = null;

	onMount(async () => {
	  getUser();
		getArchiveDetails();
		getAircraftTypes();
		getAircraft();
		getEquipmentTypes();
	  getEquipment();
	});

	const getArchiveDetails = async () => {
	  const response = await fetch('/api/archive/details', {
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
				archive_details = null;
	    }
	  } else {
	    archive_details = await response.json();
	  }
	}

	const getAircraft = async () => {
	  const response = await fetch('/api/aircraft', {
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

	function registerNewEquipment() {
		equipment_dialog.raise();
	}

	function registerNewAircraft() {
		aircraft_dialog.raise();
	}

	function upload(name, uuid) {
		upload_dialog.raise(name, uuid);
	}

	function deleteEquipment(name, uuid) {
		delete_dialog.raise(name, uuid);
	}

	const archive = async () => {
		document.body.style.cursor='wait';
		document.getElementById('wait').style.visibility = 'visible';

    const response = await fetch('/api/archive', {
      method: "get",
      withCredentials: true,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
    });
    if (!response.ok) {
      notifier.danger('Request to generate new archive failed.');
    } else {
			notifier.success('Archive generation succeeded.');

			document.body.style.cursor='default';
			document.getElementById('wait').style.visibility = 'hidden';

			// Reload the new archive name
			getArchiveDetails();
		}
	}

	function refresh() {
		sleep(50).then(() => {
			getAircraft();
			getAircraftTypes();
			getEquipment();
			getEquipmentTypes();
		})
	}
</script>

<svelte:head>
	<title>WCFC Flight Equipment</title>
</svelte:head>


	{#if equipment && equipmentTypes && aircraft && aircraftTypes}
		<table id="equipment">
			<thead>
				<tr>
					{#if $user &&  $adminState == 'on' && ! $user.anonymous}
						<th class='blank'>
							<span class='newLabel'>New :&nbsp;</span>
							<button on:click={() => registerNewEquipment()}>Equipment</button>
							<button on:click={() => registerNewAircraft()}>Aircraft</button>
							<button on:click={() => archive()}>Generate Archive</button>
						</th>
					{:else}
						<th class='blank'>&nbsp;</th>
					{/if}
					{#each aircraftTypes as type }
						<th class='type' colspan={type.count}>
							<span class='label'>{type.label}</span>
						</th>
					{/each}
				</tr>

				<tr>
					<th class='sticky'>Equipment</th>
					{#each aircraft as {registration, uuid, hasDocument } }
						<th class='reg sticky'>
							{#if $user &&  $adminState == 'on' && ! $user.anonymous}
								<button type="button" class='link admin' on:click={upload(registration, uuid)} aria-label="Upload document for {registration}">
									{registration}
									{#if hasDocument}
										<img src='document_white.png' alt='' aria-hidden="true">
									{/if}
								</button>
							{:else}
								{#if hasDocument}
									<button type="button" class='link' on:click={fetchFile(uuid)} aria-label="View document for {registration}">
										{registration} <img src='document_white.png' alt='' aria-hidden="true">
									</button>
								{:else}
									<span class='no_link'>{registration}</span>
								{/if}
							{/if}
						</th>
					{/each}
				</tr>
			</thead>
			<tbody>
				{#key aircraft}
					{#each equipmentTypes as {mtype, label} }
						<tr><td><span class='label'>{label}</span></td></tr>
						{#each equipment as { name, type, uuid, hasDocument } }
							{#if mtype === type}
								<tr class='detail'>
									{#if $user &&  $adminState == 'on' && ! $user.anonymous}
										<td>
											{#if hasDocument}
												<span class='equipment link admin'>
													<button type="button" on:click={upload(name, uuid)} aria-label="Upload document for {name}">
														{name}<img src='document.png' alt='' aria-hidden="true">
													</button>
													<button type="button" on:click={deleteEquipment(name, uuid)} aria-label="Delete {name}">
														<img src='delete.png' alt='' aria-hidden="true">
													</button>
												</span>
											{:else}
												<button type="button" class='equipment link admin' on:click={upload(name, uuid)} aria-label="Upload document for {name}">
													{name}
												</button>
											{/if}
										</td>
									{:else}
										{#if hasDocument}
											<td>
												<button type="button" class='equipment link' on:click={fetchFile(uuid)} aria-label="View document for {name}">
													{name}
													<img src='document.png' alt='' aria-hidden="true">
												</button>
											</td>
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
			</tbody>
		</table>

		<div>
		<center>
			{#if archive_details}
				<a href="/api/archive/download">
					Download Full Manual Archive // {archive_details["name"]}<br>
					<div class="archive_details">Created : {archive_details["created"]}</div>
					<div class="archive_details">Size : {archive_details["size"]}</div>
				</a>
			{:else}
					<div class="archive_details">There is no archive available at this time.</div>
			{/if}
		</center>
		</div>
	{/if}

	<UploadDialog bind:this="{upload_dialog}" on:modify on:message={refresh}/>
	<NewEquipmentDialog bind:this="{equipment_dialog}" on:modify on:message={refresh}/>
	<DeleteEquipmentDialog bind:this="{delete_dialog}" on:modify on:message={refresh}/>
	<NewAircraftDialog bind:this="{aircraft_dialog}" on:modify on:message={refresh}/>

	<div id='wait'>
		<Circle2 size="100" color="#7887a2" unit="px"></Circle2>
	</div>

<style>
.archive_details {
	padding-right: 10px;
	padding-left: 10px;
	display: inline-block;
	color: blue;
}
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
a {
	text-decoration: none;
	color: blue;
	padding: 10px;
	border-radius: 35%;
}
#equipment .newLabel { font-size: 1.3em; color: crimson }
#equipment .equipment { text-align: right; float: right; }
#equipment .link { cursor: pointer; font-size: 1.0em }
#equipment .no_link { cursor: pointer; font-size: 1.0em }
#equipment button.link {
	background: none;
	border: none;
	cursor: pointer;
	font-size: 1.0em;
	color: inherit;
	padding: 0;
}
#equipment button.link:focus {
	outline: 2px solid #005fcc;
	outline-offset: 2px;
}
#equipment .equipment button {
	background: none;
	border: none;
	cursor: pointer;
	padding: 2px;
	margin: 0 2px;
}
#equipment .equipment button:focus {
	outline: 2px solid #005fcc;
	outline-offset: 2px;
}
#equipment .label { font-weight: bold; font-size: 1.2em }
#equipment .detail { text-align: center; }
#equipment .detail:hover {background-color: #ddd;}
#equipment .blank { background-color:rgba(0, 0, 0, 0); width: 30%; }
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
#equipment .sticky {
	position: sticky;
	top: 0; /* Don't forget this, required for the stickiness */
}

@media (min-width: 480px) {
}
</style>
