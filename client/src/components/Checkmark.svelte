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
    <td class='control'>
      <button type="button" class="icon-button" on:click={removeEquipment} aria-label="Remove equipment">
        <img src='checkmark-red.png' alt='' aria-hidden="true">
      </button>
    </td>
  {:else}
    <td class='control'>
      <button type="button" class="icon-button" on:click={addEquipment} aria-label="Add equipment">
        <img src='unchecked-red.png' alt='' aria-hidden="true">
      </button>
    </td>
  {/if}
{:else}
  {#if equipedWith() }
    <td class='label'><img src='checkmark.png' alt='Equipment installed'></td>
  {:else}
    <td class='label'><img src='unchecked.png' alt='Equipment not installed'></td>
  {/if}
{/if}

<style>
.control {
  cursor: pointer;
}

.icon-button {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  display: inline-block;
}

.icon-button:focus {
  outline: 2px solid #005fcc;
  outline-offset: 2px;
}

.icon-button img {
  display: block;
}
</style>
