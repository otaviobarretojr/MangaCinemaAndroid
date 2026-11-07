const CACHE = 'gabriel-le-v4-ilustrado-20261107';
const STATIC = ['./','./index.html','./styles.css','./app.js','./manifest.webmanifest','./assets/icon-192.png','./assets/icon-512.png','./books/catalog.json','./books/gabriel-e-o-portal-misterioso.gbrbook',...Array.from({length:19},(_,i)=>'./assets/portal/p'+String(i+1).padStart(2,'0')+(i===2?'.jpg':'.webp'))];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(STATIC)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()));
});
// Conteúdo e código: atualização online primeiro. Artes: abrir do cache primeiro.
self.addEventListener('fetch', event => {
 const request=event.request;
 if(request.method!=='GET'||new URL(request.url).origin!==self.location.origin)return;
 const path=new URL(request.url).pathname;
 const changing=/\\/(?:index\\.html|app\\.js|styles\\.css|books\\/catalog\\.json)$/.test(path)||path.endsWith('/');
 if(changing){
  event.respondWith(fetch(request).then(response=>{
   if(response.ok){const copy=response.clone();event.waitUntil(caches.open(CACHE).then(cache=>cache.put(request,copy)));}
   return response;
  }).catch(()=>caches.match(request).then(cached=>cached||caches.match('./index.html'))));
  return;
 }
 event.respondWith(caches.match(request).then(cached=>cached||fetch(request).then(response=>{
  if(response.ok){const copy=response.clone();event.waitUntil(caches.open(CACHE).then(cache=>cache.put(request,copy)));}
  return response;
 })));
});