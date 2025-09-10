import { writable } from "svelte/store";
export const user = writable(null);
export const uuid = writable(null);
export const adminState = writable("off");
