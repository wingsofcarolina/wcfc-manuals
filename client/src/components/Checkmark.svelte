<script>
  import { onMount } from 'svelte'
  import { createEventDispatcher } from 'svelte';
  import { user, adminState } from '../store.js'

	const dispatch = createEventDispatcher();

  export let uuid = null;
  export let aircraft = null;

  onMount(async () => {
  });

  const modify = async (action) => {
    var json = JSON.stringify({
      aircraft: aircraft.registration,
      uuid: uuid
    });

	  const response = await fetch('/api/aircraft/' + action , {
	    method: "PATCH",
	    withCredentials: true,
	    headers: {
	      'Accept': 'application/json',
        'Content-Type': 'application/json'
	    },
      body: json
	  });
	  if (!response.ok) {
	    if (response.status == 401) {
	      console.log('User not authenticated, redirecting to Slack');
	      goto('login');
	    } else {
	      notifier.danger('Retrieve of equipment list failed.');
	    }
	  } else {
      dispatch('message', {command: 'refresh'});
    }
	}

  function addEquipment() {
    modify('add');
  }

  function removeEquipment() {
    modify('remove');
  }

  function equipedWith() {
    if (aircraft.equipment === null) {
      return false;
    } else {
      for(var i=0; i < aircraft.equipment.length; i++) {
        if (aircraft.equipment[i] === uuid) {
          return true; // break
        }
      }
      return false;
    }
  }
</script>

{#if $user &&  $adminState == 'on' && ! $user.anonymous}
  {#if equipedWith() }
    <td class='control'><img src='checkmark-red.png' alt='+' on:click={removeEquipment}></td>
  {:else}
    <td class='control'><img src='unchecked-red.png' alt='x' on:click={addEquipment}></td>
  {/if}
{:else}
  {#if equipedWith() }
    <td class='label'><img src='checkmark.png' alt='+'></td>
  {:else}
    <td class='label'><img src='unchecked.png' alt='x'></td>
  {/if}
{/if}

<style>
.label {}
.control {
  cursor: pointer;
}
</style>
