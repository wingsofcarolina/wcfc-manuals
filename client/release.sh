#!/bin/bash
npm run build

rm -rf ../server/src/main/resources/assets/*
cp -r .svelte-kit/output/client/* ../server/src/main/resources/assets
cp -r .svelte-kit/output/client/* ../server/target/classes/assets

echo "Deployed."
