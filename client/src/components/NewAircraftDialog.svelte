<script>
  import { notifier } from '@beyonk/svelte-notifications'
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let visible = false;

  let type = null;
  let registration = null;

  export const raise = () => {
    type = null;
    registration = null;
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

  const registerNewAircraft = async () => {
    visible = false;

    if (type == null || registration == null) {
      notifier.danger("All values must be provided.")
    } else {
      var json = JSON.stringify({
        type: type,
        registration: registration
      });
      const response = await fetch('/api/aircraft/register', {
        method: 'post',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: json
      });

      if (response.ok) {
        notifier.success('New aircraft registered successfully');
        refresh();
      } else {
        notifier.danger('New aircraft registration failed.');
      }
    }
  }
</script>


<div id='newAircraftDialog' class='dialog' style="visibility : {visible ? 'visible' : 'hidden'}">
  <div class='dialog_contents'>
    <div class='dialog_label'>Register Aircraft Document</div>
    <br>
    <select name="type" id="type" bind:value={type}>
      <option value="C152">Cessna 152</option>
      <option value="PA28">Warrior</option>
      <option value="C172">Cessna 172</option>
      <option value="M20J">Mooney M20J</option>
    </select>
    <br>
    <p>
    <input id="registration" type="text" bind:value={registration}>
    </p>
    <br>
    <button on:click={cancelUploadDialog.bind()}>Cancel</button>
    <input type="submit" value="Submit" on:click={registerNewAircraft.bind()}>
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
