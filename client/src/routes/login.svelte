<script>
	import { onMount } from 'svelte';
	import { goto } from '@sapper/app';
	import { notifier } from '@beyonk/svelte-notifications'
	import VerificationCode from '../components/VerificationCode.svelte'

	let code;
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

	const verifyUser = async (code) => {
		const response = await fetch('/api/member/verify/' + code, {
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
			email = null;
			warning = true;
			goto('/equipment');
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

			<p> Manuals are only available to active WCFC members. If you <i>are</i> a
			WCFC member, simply enter your email address in the field  below and click
			the "Submit Login" button. If your address is found in the database an
			email will be sent to the registered address. That email will have a
			verification code which is used to verify your login. </p>

			<p> This is intended to be a one-time operation, but if for some reason
			you clear the cookies in your browser, or you attempt to access manuals
			from a different system, you will have to re-authenticate since your
			credentials are stored in a browser cookie. </p>

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
	{:else}
		<div class="narrow">

			<div class=warning> If your address is registered with the system, an
			email has been sent to the address entered. The email will have a
			six-digit verification code. Enter that code below and click on the
			"Verify Code" button to validate your login.</div>

			<div class=verification>
				<VerificationCode length="{6}" bind:code="{code}" />
				<input id="submit" type="submit" value="Verify Code" on:click={() => verifyUser(code)}>
			</div>

			<div class=warning> If you do not use the verification code within about
			<i>two hours</i> the system will purge your verification code and an
			attempt to use it at that point will fail. If you attempt to use the code
			a second time, it will fail.</div>

			<div class=warning> Once logged in, you should not have to request
			verification again on the browser used for verification. A cookie will be
			placed on that browser so that the system will recognize it the next time
			you come to the site.</div>

		</div>
	{/if}
</center>
<style>
.verification {
	width: 350px;
	margin: 50px;
}
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
