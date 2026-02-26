// Customer Home JS (no module import, uses toast() from ../admin/assets/admin.js)

const VND = (n) => new Intl.NumberFormat('vi-VN').format(n) + '₫';

function card(p){
  return `
    <article class="p-card">
      <div class="thumb"></div>
      <div class="body">
        <div class="title">${p.name}</div>
        <div class="meta">
          <span>${p.cat}</span>
          <span class="price"><b>${VND(p.price)}</b>${p.old ? `<span class="strike">${VND(p.old)}</span>` : ''}</span>
        </div>
      </div>
      <div class="footer">
        <span class="pill">${p.tag}</span>
        <button class="btn" data-add>Thêm giỏ</button>
      </div>
    </article>
  `;
}

const best = [
  {name:"Jeans Slim Fit Co Giãn", cat:"Jeans", price:649000, old:799000, tag:"AIRFLEX"},
  {name:"Polo Basic Dệt Mịn", cat:"Polo", price:399000, old:null, tag:"New"},
  {name:"Áo Thun Cotton 220gsm", cat:"Áo thun", price:329000, old:389000, tag:"Best"},
  {name:"Sơ Mi Oxford Dễ Ủi", cat:"Sơ mi", price:459000, old:null, tag:"Office"},
];

const news = [
  {name:"Jeans Lightweight 10oz", cat:"Jeans", price:629000, old:null, tag:"Lightweight"},
  {name:"Polo Dệt Texture", cat:"Polo", price:429000, old:499000, tag:"New"},
  {name:"Hoodie Nỉ Mỏng", cat:"Hoodie", price:559000, old:null, tag:"Drop"},
  {name:"Áo Thun Oversize", cat:"Áo thun", price:349000, old:null, tag:"Trend"},
];

const outlet = [
  {name:"Áo Thun Basic (Outlet)", cat:"Áo thun", price:199000, old:329000, tag:"199K"},
  {name:"Quần Short Kaki (Outlet)", cat:"Short", price:299000, old:459000, tag:"299K"},
  {name:"Sơ Mi Flannel (Outlet)", cat:"Sơ mi", price:399000, old:559000, tag:"399K"},
  {name:"Polo Mùa Trước (Outlet)", cat:"Polo", price:299000, old:429000, tag:"299K"},
];

document.getElementById('bestGrid').innerHTML = best.map(card).join('');
document.getElementById('newGrid').innerHTML  = news.map(card).join('');
document.getElementById('outletGrid').innerHTML = outlet.map(card).join('');

document.getElementById('y').textContent = new Date().getFullYear();

document.addEventListener('click', (e)=>{
  const add = e.target.closest('[data-add]');
  if(add) toast('Đã thêm vào giỏ (demo)');
});