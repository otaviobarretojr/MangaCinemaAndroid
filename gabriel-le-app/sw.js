const CACHE = 'gabriel-le-v3-ilustrado-20261107';
const STATIC = ['./','./index.html','./styles.css','./app.js','./manifest.webmanifest','./assets/icon-192.png','./assets/icon-512.png','./books/catalog.json','./books/gabriel-e-o-portal-misterioso.gbrbook',...Array.from({length:19},(_,i)=>'./assets/portal/p'+String(i+1).padStart(2,'0')+(i===2?'.jpg':'.webp'))];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(STATIC)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()));
});
self.addEventListener('fetch', event => {
  if(event.request.method !== 'GET' || new URL(event.request.url).origin !== self.location.origin) return;
  event.respondWith(caches.match(event.request).then(cached => cached || fetch(event.request).then(resp=>{
    if(resp.ok){const copy=resp.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy));}
    return resp;
  }).catch(()=>caches.match('./index.html'))));
});