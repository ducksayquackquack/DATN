// set active menu by matching pathname
(function(){
  const path = location.pathname.replace(/\\/g,'/'); // windows fix
  document.querySelectorAll('.nav a[data-path]').forEach(a=>{
    const p = a.getAttribute('data-path');
    if(p && path.endsWith(p)) a.classList.add('active');
  });
})();

// demo confirm delete
function confirmDelete(name){
  return confirm(`Xoá "${name}"? Hành động này không thể hoàn tác.`);
}

// quick toast (optional)
function toast(msg){
  let t = document.getElementById('toast');
  if(!t){
    t = document.createElement('div');
    t.id='toast';
    t.style.cssText = `
      position:fixed;left:50%;bottom:16px;transform:translateX(-50%);
      background:rgba(18,20,26,.95);border:1px solid rgba(255,255,255,.10);
      padding:10px 12px;border-radius:999px;box-shadow:0 10px 30px rgba(0,0,0,.35);
      color:rgba(238,242,255,.95);font-size:13px;z-index:9999;display:none;
    `;
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.style.display='block';
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(()=> t.style.display='none', 1400);
}