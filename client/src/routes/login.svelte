<script>
	import { onMount } from 'svelte';
	import { goto } from '@sapper/app';
	import { notifier } from '@beyonk/svelte-notifications'

	let email = null;
	let warning = false;

	onMount(function() {
	});

	const sendMessage = async () => {
		if (email == null || email === "") {
			notifier.danger('Email address missing, but required.');
		}
		const response = await fetch('/api/member/email/' + email, {
			method: "get",
			withCredentials: true,
			headers: {
				'Accept': 'application/json',
				'Content-Type': 'application/json'
			}
		});

		if (!response.ok) {
			notifier.warning('Request failed, contact webmaster for help.');
		} else {
			notifier.success('Authentication requested.');
			email = null;
			warning = true;
		}
	}
</script>

<svelte:head>
	<title>Login</title>
</svelte:head>

<div class=title>Authenticate</div>
<hr class="highlight">

<center>
	{#if ! warning}
		<div class="narrow">

			<p> Class content is intended to be available to only the active WCFC
			members. If you <b>are</b> a WCFC member, simply enter your email address in
			the field  below and click the "Submit Login" button. If your email address
			is found in the system an email will be sent to the registered address. That
			email will have a URL which will return you to the system with your
			authentication browser cookie set. This is intended to be a one-time
			operation, but if for some reason you clear the cookies in your browser you
			may have to re-authenticate since your credentials are stored in a browser
			cookie. </p>

		</div>

		<div class="section">
			<div class="contact_block">
				<div class="contact_info">
					<div class="contact_row">
						<input type="text" id="email" name="email" placeholder="Email"
						size=40 bind:value={email}>
					</div>
					<input id="submit" type="submit" value="Submit Login" on:click={() => sendMessage()}>
				</div>
			</div>
		</div>

		<p>Or authenticate with Slack</p>
		<div class="auth">
			<a href="https://slack.com/oauth/v2/authorize?user_scope=identity.basic,identity.email&client_id=REDACTED">
				<img alt="Sign in with Slack" height="40" width="172" src="https://platform.slack-edge.com/img/sign_in_with_slack.png"
				 srcset="https://platform.slack-edge.com/img/sign_in_with_slack.png 1x, https://platform.slack-edge.com/img/sign_in_with_slack@2x.png 2x" />
			</a>
		</div>
	{:else}
		<div class="narrow">
			<div class=warning> An email has been sent to the email address entered. Close this page
				and use the link in the verification email to log into the system.</div>

			<img src="/icons8-email.png" alt="Check Your Email!">

			<div class=warning> If you do not use the verification URL within about <i>two
				hours</i> the system will purge your verification code and the link will fail. If
				you attempt to use the URL a second time, it will fail.</div>

			<div class=warning> Once logged in, you should never have to request verification
				or use a verification URL again. A cookie will be placed on your browser so
				that the system will recognize you the next time you come to the site.</div>

			<div class=warning> You can close this browser page or tab now.</div>
		</div>
	{/if}
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
.section {
  width: 100%;
  margin-bottom: 3em;
}
.narrow p {
	width: 70%;
	text-align: left;
}
.warning {
	width: 70%;
	text-align: left;
	margin-bottom: 20px;
}
.contact_block {
  display: flex;
  justify-content: space-around;
  margin: 20px;
  font-size: 20px;
}
.contact_info {
  text-align: center;
}

input, textarea {
	font-family: 'Courier', sans-serif;
	padding: 10px;
	margin: 10px 0;
	border:0;
	box-shadow:0 0 15px 4px rgba(0,0,0,0.06);
	font-family: inherit;
}
input[type=submit] {
		padding:5px 15px;
		background:#ccc;
		border:0 none;
		cursor:pointer;
		-webkit-border-radius: 5px;
		border-radius: 5px;
		height: 50px;
		width: 100%;
}
</style>
