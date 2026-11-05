/* Coast Rush Bitmap Edition — optional pre-rendered sprite layer.
   Missing or failed assets always fall back to the original Canvas renderer. */
(function(){
 'use strict';
 const ROOT='sprites/bitmap/';
 const registry=new Map();
 const stats={loaded:0,failed:0,drawn:0,fallback:0};
 let ready=false;
 function loadOne(key,entry){
  const views=entry&&entry.views||{};
  Object.keys(views).forEach(function(view){
   const spec=views[view],url=typeof spec==='string'?spec:spec&&spec.src;
   if(!url)return;
   const img=new Image();
   const id=key+'|'+view;
   img.decoding='async';
   img.onload=function(){registry.set(id,{img:img,spec:typeof spec==='string'?{}:spec});stats.loaded++};
   img.onerror=function(){stats.failed++};
   img.src=ROOT+url;
  });
 }
 function loadManifest(){
  fetch(ROOT+'manifest.json',{cache:'no-cache'}).then(function(r){
   if(!r.ok)throw new Error('bitmap manifest unavailable');
   return r.json();
  }).then(function(m){
   Object.keys(m.sprites||{}).forEach(function(k){loadOne(k,m.sprites[k])});
   ready=true;
  }).catch(function(){ready=true});
 }
 function poseName(p){
  return ({0:'straight',1:'turn-right',2:'turn-left',3:'drift-right',4:'drift-left'})[p|0]||'straight';
 }
 function draw(ctx,key,view,x,y,w,h){
  const e=registry.get(key+'|'+view)||registry.get(key+'|straight');
  if(!e||!e.img.complete||!e.img.naturalWidth){stats.fallback++;return false}
  const s=e.spec||{},iw=e.img.naturalWidth,ih=e.img.naturalHeight;
  const dw=w*(s.scaleX||1),dh=h*(s.scaleY||1);
  const px=s.pivotX==null?.5:s.pivotX,py=s.pivotY==null?.86:s.pivotY;
  ctx.drawImage(e.img,x-dw*px,y-dh*py,dw,dh);stats.drawn++;return true;
 }
 window.CRBitmap={
  drawVehicle:function(ctx,v,pose,x,y,w,h){
   const id='vehicle-'+(v<0?'player':v);
   return draw(ctx,id,poseName(pose),x,y,w,h);
  },
  status:function(){return {ready:ready,loaded:stats.loaded,failed:stats.failed,drawn:stats.drawn,fallback:stats.fallback,assets:registry.size}},
  preload:loadManifest
 };
 loadManifest();
})();