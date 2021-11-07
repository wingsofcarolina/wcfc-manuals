<script context="module">
	// retain module scoped expansion state for each tree node
	const _expansionState = {
		/* treeNodeId: expanded <boolean> */
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
      // var fileURL = URL.createObjectURL(data);
      // window.open(fileURL, 'wcfc-manuals');
			view.setPdf(data);

			document.body.style.cursor='default';
			document.getElementById('wait').style.visibility = 'hidden';
    }
  }
</script>

<script>
	import { goto } from '@sapper/app'
	import { Circle2 } from 'svelte-loading-spinners'
	import { uuid } from '../store.js'

//	import { slide } from 'svelte/transition'
	export let tree
	const {label, link, open, children} = tree

	let data = null;

	let expanded = _expansionState[label] || false || open
	const toggleExpansion = () => {
		expanded = _expansionState[label] = !expanded
	}
	$: arrowDown = expanded


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
</script>

<ul><!-- transition:slide -->
	<li>
		{#if children}
			<span class="arrow" class:arrowDown on:click={toggleExpansion}>
				<img src="folder.png" alt="">
				<span class='label'>{label}</span>
			</span>
			{#if link}
				<img src='document.png' alt='Document' on:click={() => fetchFile(link)}>
			{/if}

			{#if expanded}
				{#each children as child}
					<svelte:self open=true tree={child} />
				{/each}
			{/if}
		{:else}
			<span>
				<span class="no-arrow"/>
				{#if link}
					<span class='label'>{label}</span>
					<img src='document.png' alt='Document' on:click={() => fetchFile(link)}>
				{:else}
					{label}
				{/if}
			</span>
		{/if}
	</li>
</ul>

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
	img {
		cursor: pointer;
	}
	ul {
		margin: 0;
		list-style: none;
		padding-left: 1.2rem;
		user-select: none;
	}
	.label { margin-right: 10px; }
	.no-arrow { padding-left: 1.0rem; }
	.arrow {
		cursor: pointer;
		display: inline-block;
		/* transition: transform 200ms; */
	}
	.arrowDown {  } /* transform: rotate(90deg); */
</style>
