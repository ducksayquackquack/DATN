// Employee dashboard JS (uses toast() from ../admin/assets/admin.js)

document.getElementById('year').textContent = new Date().getFullYear();

document.addEventListener('click', (e)=>{
  const t = e.target.closest('[data-action]');
  if(!t) return;
  toast(t.getAttribute('data-action'));
});