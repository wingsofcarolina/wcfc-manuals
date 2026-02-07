<script>
	// No JavaScript needed for static contact page
</script>

<svelte:head>
	<title>Contact</title>
</svelte:head>

<div class=title>Contact</div>
<hr class="highlight">

<center>
<div class="narrow">

	<p>We welcome feedback, bug reports, and feature requests from our users.
	To help us better track and manage issues, we've moved our support system
	to GitHub Issues.</p>

	<p>Please visit our <strong>GitHub Issues tracker</strong> to:</p>
	<ul>
		<li>Report bugs or technical problems</li>
		<li>Request new features or improvements</li>
		<li>Ask questions about the system</li>
		<li>View existing issues and their status</li>
	</ul>

	<div class="github-link">
		<a href="https://github.com/wingsofcarolina/wcfc-manuals/issues" target="_blank" rel="noopener noreferrer">
			<button class="github-button">
				<img src="/github-icon.svg" alt="GitHub" class="github-icon" />
				Open GitHub Issues
			</button>
		</a>
	</div>

	<p><em>This helps us organize feedback more effectively and allows the community
	to see what issues are being worked on and contribute to discussions.</em></p>

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
.narrow ul {
	width: 70%;
	text-align: left;
	font-size: 1.2em;
	margin: 1em 0;
}
.narrow li {
	margin: 0.5em 0;
}
.github-link {
	margin: 2em 0;
	text-align: center;
}
.github-button {
	background-color: rgb(40, 90, 149);
	color: white;
	border: none;
	padding: 15px 30px;
	font-size: 1.2em;
	border-radius: 8px;
	cursor: pointer;
	text-decoration: none;
	display: inline-block;
	transition: background-color 0.3s ease;
	box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}
.github-button:hover {
	background-color: rgb(30, 70, 120);
	box-shadow: 0 6px 12px rgba(0,0,0,0.3);
}
.github-link a {
	text-decoration: none;
}
.github-icon {
	width: 20px;
	height: 20px;
	margin-right: 8px;
	vertical-align: middle;
	filter: brightness(0) invert(1);
}
</style>
