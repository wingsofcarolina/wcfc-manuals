<script>
	import Nav from '../components/Nav.svelte';

	export let segment;
</script>

<style>
main {
	position: relative;
	max-width: 75em;
	background-color: white;
	padding: 2em;
	margin: 0 auto;
	box-sizing: border-box;
	margin-bottom: 50px;
}
footer {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  background-color: rgb(233, 238, 243);
  color: black;
  text-align: center;
	border-top: 2px solid rgba(40, 90, 149, 0.2);
	font-size: 12px;
}
</style>

<Nav {segment}/>

<main>
	<slot></slot>
</main>
<footer>
	<p>Copyright &copy; 2021, Wings of Carolina; All rights reserved.</p>
</footer>
