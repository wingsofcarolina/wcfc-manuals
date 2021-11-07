<script>
  import { notifier } from '@beyonk/svelte-notifications'
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let visible = false;
  export let item;
  export let section;

  let label = null;
  let lesson = null;
  let files = null;

  export const raise = () => {
    label = item.label;
    lesson = item.lesson;
    visible = true
  }

  const cancelUploadDialog = async () => {
    // var el = document.getElementById("uploadDialog");
    // if (el) el.style.visibility = "hidden";
    visible = false;
  }

  const refresh = async (item) => {
    var request = { command : 'refresh' };
    dispatch('modify', request );
  }

  const uploadNewHandout = async () => {
    visible = false;

    if (files == null || label == null || lesson == null) {
      notifier.danger("All values must be provided.")
    } else {
      const formData = new FormData();
      formData.append('label', label);
      formData.append('path', item.path);
      formData.append('section', section);
      formData.append('lesson', lesson);
      for (var pair of formData.entries()) {
          console.log(pair[0]+ ', ' + pair[1]);
      }
      const response = await fetch('/api/update', {
          method: 'post',
          body: formData
      });
      if (response.ok) {
        notifier.success('Entry modified successfully');
        refresh();
      } else {
        notifier.danger('Entry modification failed');
      }
    }
  }
</script>


<div id='uploadDialog' class='dialog' style="visibility : {visible ? 'visible' : 'hidden'}">
  <div class='dialog_contents'>
    <div class='dialog_label'>Handout Display Name</div>
    <input bind:value={label}><br>
    <div class='dialog_label'>Class Number</div>
    <input width=10 bind:value={lesson}><br>
    <p>
    <button on:click={cancelUploadDialog.bind()}>Cancel</button>
    <input type="submit" value="Submit" on:click={uploadNewHandout.bind()}>
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
