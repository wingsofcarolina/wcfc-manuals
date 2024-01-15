<script>
	import { goto } from '@sapper/app';
  import { NotificationDisplay } from '@beyonk/svelte-notifications'
  import { user, adminState } from '../store.js'
  import Switch from '../components/Switch.svelte'
	import MediaQuery from "../components/MediaQuery.svelte";

	export let segment;

  const home = async () => {
    goto('/');
  }
</script>

<div class="banner">
  <div class=branding>
    <div class=logo on:click={home.bind()}><img src=/WCFC-logo.jpg alt="WCFC Manuals"></div>
    <div class=title>WCFC Flight Manuals</div>
  </div>
	<MediaQuery query="(min-width: 1001px)" let:matches>
		{#if matches && $user &&  $user.admin == true && $user.anonymous == false && segment === "equipment"}
		  <div class="switch">
		    <Switch bind:value={$adminState} label="Admin Mode" design="inner"/>
		  </div>
		{/if}
	</MediaQuery>
</div>
<div class="nav">
	<div class="left"><a class:selected='{segment === undefined}' href='.'>home</a></div>
	<div class="left"><a class:selected='{segment === "equipment"}' href='equipment'>equipment</a></div>
	<div class="left"><a class:selected='{segment === "contact"}' href='contact'>contact</a></div>
  <div class="left"><a class:selected='{segment === "about"}' href='about'>about</a></div>

	<MediaQuery query="(min-width: 601px)" let:matches>
		{#if matches && $user &&  ! $user.anonymous}
			<div class="right">
	      <div class="user">
	        {$user.name}
	      </div>
	    </div>
		{/if}
	</MediaQuery>

  <NotificationDisplay />
</div>

<style>
  .logo {
    float:left;
    padding:10px;
    padding-bottom: 0px;
    cursor: pointer;
  }
  .title {
    float:right;
    margin-top:10px;
  }
	.nav {
		border-bottom: 2px solid rgba(40, 90, 149, 0.2);
		font-weight: 300;
		padding: 0 1em;
		height:100%;
		margin: 0;
		background: #ffffff;
	}
  .banner {
    display:inline-block;
    vertical-align: middle;
    font-size: 2em;
    font-weight: 300;
    text-align: top;
    width: 100%;
  }
	/* clearfix */
	.nav::after {
		content: '';
		display: block;
		clear: both;
	}
	.left {
		display: block;
		float: left;
		color: #114444;
		text-decoration: none;
	}
  .right {
    display: block;
    float: right;
    color: #114444;
    text-decoration: none;
  }
  .switch {
    display: block;
    float: right;
    color: #114444;
    text-decoration: none;
    padding: 5px;
    font-size: 12px;
  }
  .user {
    padding: 1em 0.5em;
  }
	.selected {
		position: relative;
		display: inline-block;
	}
	.selected::after {
		position: absolute;
		content: '';
		width: calc(100% - 1em);
		height: 2px;
		background-color: rgba(40, 90, 149, 1);
		display: block;
		bottom: -1px;
	}
  .branding {
    display: inline-block;
  }
	a {
		text-decoration: none;
		padding: 1em 0.5em;
		display: block;
	}
</style>
