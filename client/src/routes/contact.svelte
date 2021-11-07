<script>
	import { onMount } from 'svelte';
	import { goto } from '@sapper/app';
	import { notifier } from '@beyonk/svelte-notifications'
	import { user } from '../store.js'
	import { getUser } from '../common.js'

	let name;
	let phone;
	let email;
	let message;

	onMount(function() {
		getUser();
	});

	const sendMessage = async () => {
    if (name == null || name === "") {
      notifier.danger('Name missing, but required.');
    } else if (email == null || email === "") {
      notifier.danger('Email address missing, but required.');
    } else if (message == null || message === "") {
      notifier.danger('Message missing, but required.');
    } else {
			if (phone == null) {
				phone = "NONE";
			}
      var json = JSON.stringify({
        name: name,
        phone: phone,
        email: email,
        message: message
      });

      const response = await fetch('/api/contact', {
        method: "post",
        withCredentials: true,
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: json
      });

      if (!response.ok) {
        notifier.warning('Sending of message failed, but try again because we really would like to hear from you.');
      } else {
        notifier.success('Message sent successfully.');
				name = null;
        phone = null;
        email = null;
        message = null;
      }
    }
  }
</script>

<svelte:head>
	<title>Contact</title>
</svelte:head>

<div class=title>Contact</div>
<hr class="highlight">

<center>
<div class="narrow">
	<p> Use the form below to send a message to the administrators of the Groundschool
		server. We welcome messages, particulary if you see a problem that we might be
		able to address. Reporting issues/problems aids us in creating a better service
		for you and your fellow groundschool participants.

		The message is sent to a channel in the Groundschool Slack workspace where it
		will be addressed by one of the administrators.
	</p>
</div>
</center>

<div class="section">
	<div class="contact_block">
		<div class="contact_info">
			<div class="contact_row">
				<input type="text" id="name" name="name" placeholder="Name"
				size=40 bind:value={name}>
			</div>
			<div class="contact_row">
				<input type="text" id="email" name="email" placeholder="Email"
				size=40 bind:value={email}>
			</div>
			<div class="contact_row">
				<input type="text" id="phone" name="phone" placeholder="Phone (optional)"
				size=40 bind:value={phone}>
			</div>
			<div class="contact_row">
				<textarea type="text" id="message" name="message" placeholder="Message"
				cols=41 rows=5 bind:value={message}></textarea>
			</div>
			<input id="submit" type="submit" value="Send Message" on:click={() => sendMessage()}>
		</div>
	</div>
</div>

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
.section {
  width: 100%;
  margin-bottom: 3em;
}
.narrow p {
	width: 70%;
	text-align: left;
	font-size: 1.2em;
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
