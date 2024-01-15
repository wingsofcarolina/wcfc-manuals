<script>
	import { onMount } from 'svelte'
	import { goto } from '@sapper/app'
	import { user, adminState } from '../store.js'
	import { getUser } from '../common.js'
	import TreeView from '../components/TreeView.svelte'
	import TableView from '../components/TableView.svelte'
	import MediaQuery from "../components/MediaQuery.svelte";

	let equipment = null;
	let equipmentTypes = null;
	let aircraft = null;
	let aircraftTypes = null;

	let dialog;
	let data = null;

	let tree = null;

	onMount(async () => {
	  getUser();
		if ($user == null) {
			sleep(1000).then(() => {
				if ($user == null || $user.anonymous) {
					goto('login');
				}
			})
		} else if ( $user.anonymous ) {
			goto('login');
		}

		getTree();
	});

	const sleep = (milliseconds) => {
	  return new Promise(resolve => setTimeout(resolve, milliseconds))
	}

	const getTree = async () => {
		aircraft = null;
	  const response = await fetch('/api/tree', {
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
	    tree = await response.json();
	  }
	}

</script>

<svelte:head>
	<title>WCFC Flight Equipment</title>
</svelte:head>

{#if $user != null && $user.anonymous == false}
	<MediaQuery query="(min-width: 1001px)" let:matches>
		{#if matches && tree}
			<TableView />
		{/if}
	</MediaQuery>

	<MediaQuery query="(max-width: 1000px)" let:matches>
	    {#if matches && tree}
				<TreeView {tree} />
	    {/if}
	</MediaQuery>
{:else}
	Anonymous access to this material is prohibited. Contact the
	<strong>Wings of Carolina</strong> for more information.
{/if}

<style>
@media (min-width: 480px) {
}
</style>
