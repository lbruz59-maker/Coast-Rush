const CACHE_NAME="coast-rush-v197-outrun-v188-feel-bridge-traffic-render-fix-raised-bridge-traffic-v6-mountain-cablecars-coast-train-coast-train-v2-train-on-deck-v3";
const CORE=["./","./index.html","./manifest.webmanifest","./icon.svg","../../versions/coast-rush-v188-ponts/assets/menu-music.mp3","../../versions/coast-rush-v188-ponts/assets/sunset-racers.mp3","../../versions/coast-rush-v188-ponts/assets/endless-horizon.mp3"];
self.addEventListener("install",event=>{
 event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.addAll(CORE)).then(()=>self.skipWaiting()));
});
self.addEventListener("activate",event=>{
 event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith("coast-rush-")&&key!==CACHE_NAME).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));
});
self.addEventListener("fetch",event=>{
 const req=event.request;
 if(req.method!=="GET"||new URL(req.url).origin!==self.location.origin)return;
 if(req.mode==="navigate"){
  event.respondWith(fetch(req).then(response=>{
   const copy=response.clone();caches.open(CACHE_NAME).then(cache=>cache.put("./index.html",copy));return response;
  }).catch(()=>caches.match("./index.html")));
  return;
 }
 event.respondWith(caches.match(req).then(cached=>cached||fetch(req).then(response=>{
  if(response.ok){const copy=response.clone();caches.open(CACHE_NAME).then(cache=>cache.put(req,copy));}
  return response;
 })));
});