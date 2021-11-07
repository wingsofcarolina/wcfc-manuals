<script context="module" src='/lib/webviewer.min.js'></script>
<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import { uuid } from '../store.js'

  let instance = null;
  const dispatch = createEventDispatcher();

  let lib = null;

  onMount(async () => {
    let doc = '/root/' + $uuid + '.pdf';
    console.log(doc);
    lib = (await import('@pdftron/pdfjs-express')).default;
  	const ele = document.getElementById('viewer');
    WebViewer({
      path: '/lib/public',
      initialDoc: doc // path to your document
  	}, ele).then(instance => {
      instance.UI.setHeaderItems(function(header) {
        // get the tools overlay
        const toolsOverlay = header.getHeader('toolbarGroup-Annotate').get('toolsOverlay');
        header.getHeader('toolbarGroup-Annotate').delete('toolsOverlay');
        // add the line tool to the top header
        header.getHeader('default').push({
          type: 'toolGroupButton',
          toolGroup: 'lineTools',
          dataElement: 'lineToolGroupButton',
          title: 'annotation.line',
        });
        // add the tools overlay to the top header
        header.push(toolsOverlay);
      });
      // hide the ribbons and second header
      instance.UI.disableElements(['ribbons']);
      instance.UI.disableElements(['toolsHeader']);
  		dispatch('ready', { instance })
  	})
  });
</script>

<div id='viewer'></div>


<style>
#viewer {
	width: 100%;
	height: 100vh;
}
</style>
