<script>
  import { notifier } from '@beyonk/svelte-notifications'
  import { createEventDispatcher } from 'svelte';
  import { Circle2 } from 'svelte-loading-spinners'

  const dispatch = createEventDispatcher();

  export let visible = false;

  let label = null;
  let type = null;
  let name = null;
  let identifier = null;
  let files = null;

  export const raise = (n, id) => {
    name = n;
    identifier = id;
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

  const uploadNewHandout = async () => {
    visible = false;
    document.body.style.cursor='wait';
		document.getElementById('wait').style.visibility = 'visible';

    if (files == null || identifier == null) {
      notifier.danger("All values must be provided.")
    } else {
      const formData = new FormData();
      formData.append('identifier', identifier);
      formData.append('file', files[0]);
      // for (var pair of formData.entries()) {
      //     console.log(pair[0]+ ', ' + pair[1]);
      // }
      const response = await fetch('/api/upload', {
          method: 'post',
          body: formData
      });
      if (response.ok) {
        notifier.success('File uploaded successfully');
        name = identifier = files = null;
        refresh();
      } else {
        notifier.danger('File failed to upload (not a PDF??)');
      }
      document.body.style.cursor='default';
      document.getElementById('wait').style.visibility = 'hidden';
    }
  }
</script>


<div id='uploadDialog' class='dialog' style="visibility : {visible ? 'visible' : 'hidden'}">
  <div class='dialog_contents'>
    <div class='dialog_label'>Upload Manual For ....</div>
    {#if name != null}
      <div class='dialog_label name'>{name}</div>
    {/if}
    <br>
    <input id="file" type="file" bind:files>
    <p>
    </p>
    <br>
    <button on:click={cancelUploadDialog.bind()}>Cancel</button>
    <input type="submit" value="Submit" on:click={uploadNewHandout.bind()}>
  </div>
</div>

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
.name {
  text-align: center;
  font-weight: bold;
}
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
