<script>
  import * as notifier from '@beyonk/svelte-notifications/src/notifier.js'
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let visible = false;

  let type = null;
  let name = null;

  export const raise = () => {
    type = null;
    name = null;
    visible = true;
  }

  const cancelUploadDialog = async () => {
    // var el = document.getElementById("uploadDialog");
    // if (el) el.style.visibility = "hidden";
    visible = false;
  }

  const refresh = async (item) => {
    dispatch('message', {command: 'refresh'});
  }

  const registerNewEquipment = async () => {
    visible = false;

    if (type == null || name == null) {
      notifier.danger("All values must be provided.")
    } else {
      var json = JSON.stringify({
        type: type,
        name: name
      });
      const response = await fetch('/api/equipment/register', {
        method: 'post',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: json
      });

      if (response.ok) {
        notifier.success('New equipment document registered successfully');
        refresh();
      } else {
        notifier.danger('New document registration failed.');
      }
    }
  }
</script>


<div id='newEquipmentDialog' class='dialog' style="visibility : {visible ? 'visible' : 'hidden'}">
  <div class='dialog_contents'>
    <div class='dialog_label'>Register Equipment Document</div>
    <br>
    <select name="type" id="type" bind:value={type}>
      <option value="INTERCOM">Intercoms</option>
      <option value="AUDIOPNL">Audio Panels</option>
      <option value="NAVCOM">Nav/Coms</option>
      <option value="TRANSPONDER">Transponders</option>
      <option value="GPS">GPSs</option>
      <option value="AUTOPILOT">AutoPilots</option>
      <option value="TOTALIZER">Fuel Totalizers</option>
      <option value="ENGMONITOR">Engine Monitors</option>
      <option value="DME">DMEs</option>
      <option value="OTHER">Other</option>
    </select>
    <br>
    <p>
    <input id="name" type="text" bind:value={name}>
    </p>
    <br>
    <button on:click={cancelUploadDialog.bind()}>Cancel</button>
    <input type="submit" value="Submit" on:click={registerNewEquipment.bind()}>
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
