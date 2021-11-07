<script>
	import { onMount } from 'svelte';
	import { goto } from '@sapper/app';

	onMount(function() {
	});
</script>

<svelte:head>
	<title>Login</title>
</svelte:head>

<div class=title>Authenticate</div>
<hr class="highlight">

<center>
	<div class="narrow">

		<p> This content is intended to be available to only the  members of the
		Wings of Carolina Flying Club. To access this material each member needs to
		join the WCFC Slack workspace. Contact <a
		href="mailto:board@wingsofcarolina.org">a board member</a> if you need an
		invitation to the workspace, or if you have any questions. </p>

		<p>If you <i><b>are</b></i> already a WCFC Slack workspace member, simply
		click on the button below to authenticate. This is intended to be a one-time
		operation, but if for some reason you clear the cookies in your browser you
		may have to re-authenticate since your credentials are stored in a browser
		cookie. </p> </div>

	<div class="auth">
		<a href="https://slack.com/oauth/v2/authorize?user_scope=identity.basic,identity.email&client_id=REDACTED">
			<img alt="Sign in with Slack" height="40" width="172" src="https://platform.slack-edge.com/img/sign_in_with_slack.png"
			 srcset="https://platform.slack-edge.com/img/sign_in_with_slack.png 1x, https://platform.slack-edge.com/img/sign_in_with_slack@2x.png 2x" />
		</a>
	</div>
</center>

<style>
.auth {
	margin-top: 3em;
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
.narrow p {
	width: 70%;
	text-align: left;
	font-size: 1.2em;
}
</style>
