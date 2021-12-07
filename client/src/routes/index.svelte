<script>
	import { onMount } from 'svelte'
	import { goto } from '@sapper/app'
	import { notifier } from '@beyonk/svelte-notifications'
	import { user } from '../store.js'
	import { getUser } from '../common.js'

	var data = null;

	onMount(async () => {
		getUser();
		cookieWarning();
	});

	function cookieWarning() {
		if (localStorage.getItem('cookieSeen') != 'shown') {
			var el = document.getElementById('cookie-banner');
			if (el) el.style.visibility = "visible";
			localStorage.setItem('cookieSeen', 'shown')
		}
	}

	function acceptCookie() {
		 var el = document.getElementById('cookie-banner');
		 //if (el) el.style.visibility = "hidden";
		 var fadeEffect = setInterval(function() {
			 if (!el.style.opacity) {
				 el.style.opacity = 1;
			 }
			 if (el.style.opacity > 0) {
				 el.style.opacity -= 0.1;
			 } else {
				 clearInterval(fadeEffect);
			 }
		 }, 150);
	 }
</script>

<svelte:head>
	<title>WCFC Flight Manuals</title>
</svelte:head>

	<div class="outer">
		<div class="inner">
			<div class=title>Welcome!</div>
			<hr class="highlight">

			<center>
			<div class="narrow">

				<p>Here you'll find all the manuals associated with the aircraft in the
					WCFC fleet.  </p>

				<p>We are making this information available through this site to support
				our members in their use of the aircraft in our fleet. </p>

				<p>To access this material you should be logged into the WCFC Slack
				workspace (for more information on Slack go to slack.com). This
				website uses Slack to authenticate that you are a member of WCFC. If
				you are not logged in already Slack may ask for your workspace name. If so,
				enter <b>wcfc.slack.com</b>. If you have not already joined the WCFC Slack
			  workspace, ask another member for an invitation.</p>

				<p>The links in the navigation bar above will take you to the various
				provided materials. If you have questions about the material use either
				the "contact" page referenced above in the navigation bar or go into the
				Slack workspace and ask your questions there. </p>

				<p><strong>NOTE:</strong> The PDF documents are launched into a separate
					browser tab, and therefore you <em>must</em> allow pop-ups for this
					website. Otherwise your browser may silently just not show you the
					requested document.</p>

			</div>
		  </center>
		</div>
	</div>

	<div id='cookie-banner' class='cookie-banner'>
	<p>
			By using this website, you agree to our
			<a href='about'>cookie policy</a>
		</p>
		<button class='close' on:click={acceptCookie.bind()}>&times;</button>
	</div>

<style>
button {
	text-align: center;
}
.narrow p {
	width: 70%;
	text-align: left;
	font-size: 1.2em;
}
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
.outer {
  display: flex;
	flex-direction: column;
	align-items: center;
}
.inner {
	display: flex;
	flex-direction: column;
}
@media (min-width: 480px) {
}
.cookie-banner {
	visibility: hidden;
	position: fixed;
  bottom: 60px;
  left: 10%;
  right: 10%;
  width: 80%;
  padding: 5px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #eee;
  border-radius: 5px;
  box-shadow: 0 0 2px 1px rgba(0, 0, 0, 0.2);
}
.close {
  height: 20px;
  background-color: #777;
  border: none;
  color: white;
  border-radius: 2px;
  cursor: pointer;
}
</style>
