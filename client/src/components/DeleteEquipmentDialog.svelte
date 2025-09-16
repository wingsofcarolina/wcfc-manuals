<script>
  import { notifier } from '@beyonk/svelte-notifications'
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let visible = false;

  var dialog_uuid;
  var dialog_name;

  export const raise = (name, uuid) => {
    dialog_uuid = uuid;
    dialog_name = name;
    visible = true;
  }

  const cancelDeleteDialog = async () => {
    // var el = document.getElementById("uploadDialog");
    // if (el) el.style.visibility = "hidden";
    visible = false;
  }

  const refresh = async (item) => {
    dispatch('message', {command: 'refresh'});
  }


  	const deleteEquipment = async () => {
  		const response = await fetch('/api/equipment/' + dialog_uuid, {
        method: "delete",
        withCredentials: true,
        headers: {
  				'Accept': 'application/json'
        },
      });

      if (response.ok) {
        notifier.success('Document deleted successfully');
        refresh();
      } else {
        notifier.danger('Document deletion failed.');
      }
      visible = false;
  	}
</script>

<div id='deleteEquipmentDialog' class='dialog' style="visibility : {visible ? 'visible' : 'hidden'}">
  <div class='dialog_contents'>
    <div class='dialog_label'>Delete Document</div>
    <br>
    {#if dialog_name}
      Are you sure you want to delete {dialog_name}?
    {/if}
    <br>
    <br>
    <button on:click={cancelDeleteDialog.bind()}>Cancel</button>
    <input type="submit" value="Submit" on:click={deleteEquipment.bind()}>
  </div>
</div>

<style>
.dialog {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
}
.dialog_contents {
  margin: 100px auto;
  background-color: #f2f2f2;
  border-radius: 10px;
  -webkit-border-radius: 10px;
  -moz-border-radius:  10px;
  border:1px solid #666666;
  padding:15px;
  text-align:center;
  font-weight: bold;
  font-size: 15px;
  border: 3px solid #cccccc;
  position: absolute;
  left: 50%;
  top: 100px;
  transform: translate(-50%, -50%);
  -ms-transform: translate(-50%, -50%);
  -webkit-transform: translate(-50%, -50%);
}
.dialog_label {
  text-align: left;
}
</style>
