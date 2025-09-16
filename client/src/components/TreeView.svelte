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
	import { goto } from '$app/navigation'
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
			<button type="button" class="arrow" class:arrowDown on:click={toggleExpansion} aria-label="Toggle folder {label}">
				<img src="folder.png" alt="" aria-hidden="true">
				<span class='label'>{label}</span>
			</button>
			{#if link}
				<button type="button" class="document-button" on:click={() => fetchFile(link)} aria-label="Open document for {label}">
					<img src='document.png' alt='' aria-hidden="true">
				</button>
			{/if}

			{#if expanded}
				{#each children as child}
					<svelte:self open=true tree={child} />
				{/each}
			{/if}
		{:else}
			<span>
				<span class="no-arrow"></span>
				{#if link}
					<span class='label'>{label}</span>
					<button type="button" class="document-button" on:click={() => fetchFile(link)} aria-label="Open document {label}">
						<img src='document.png' alt='' aria-hidden="true">
					</button>
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
		background: none;
		border: none;
		padding: 0;
		/* transition: transform 200ms; */
	}
	.arrow:focus {
		outline: 2px solid #005fcc;
		outline-offset: 2px;
	}
	.document-button {
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		margin-left: 5px;
	}
	.document-button:focus {
		outline: 2px solid #005fcc;
		outline-offset: 2px;
	}
	.document-button img, .arrow img {
		cursor: pointer;
	}
</style>
