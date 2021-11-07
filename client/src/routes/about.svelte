<script>
	import { onMount } from 'svelte';
	import { goto } from '@sapper/app';
	import { getUser } from '../common.js'

	onMount(function() {
		getUser();
	});
</script>

<svelte:head>
	<title>About</title>
</svelte:head>

<div class=title>About</div>
<hr class="highlight">

<center>
<div class="narrow">

	<p> This site has been put in place to facilitate the sharing of aircraft and
		equipment manuals at the Wings of Carolina Flying Club.

		If you have questions or concerns about this site direct your questions to
		us via the <a href="contact">contact</a> page. Thanks!
	</p>

	<h2>Cookies</h2>

	<p> As is the case with most websites these days, functionality is enhanced by
	the use of cookies. A cookie is used to store a cryptographically secure
	token identifying the user to the site. A browser-local storage item indicates
	whether the user has seen the cookie usage banner as is now required by the <a
	target='_blank' href='https://www.cookielaw.org/the-cookie-law/'> European
	Union</a>. </p>

	<p> <b>By continuing to use this site you implicitly agree to the use
	of this storage  on your device. </b></p>

	<h2>Technology</h2>

	<p> This tool is built by Dwight Frye using the <i>Svelte</i> JavaScript
	framework. The back-end is built using the Java <i>DropWizard</i> restful
	server framework. Both are delivered as a unit in a single <i>Docker</i>
	container. The site is integrated with the Slack system to provide notifications
	and contacts. </p>

	<hr> <p><strong>If you have problems with the system, or suggestions for new
	functionality contact us via the <a href="contact">contact</a> page
	so your input can be considered.</strong></p>
</div>
</center>

<style>
.title {
  font-size: 2em;
  text-align: center;
}
.highlight {
  height: 4px;
  margin-top: 25px;
  margin-bottom: 40px;
  width: 250px;
  border-color: rgb(40, 90, 149);
  background-color: rgb(40, 90, 149);
  border-radius: 3px;
	margin: 0px auto 50px auto;
}
.narrow p {
	width: 70%;
	text-align: left;
	font-size: 1.2em;
}
</style>
