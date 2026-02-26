document.getElementById('y').textContent = new Date().getFullYear();

const pw = document.getElementById('password');
const togglePw = document.getElementById('togglePw');

togglePw.addEventListener('click', ()=>{
  pw.type = (pw.type === 'password') ? 'text' : 'password';
  toast(pw.type === 'password' ? 'Đã ẩn mật khẩu' : 'Đã hiện mật khẩu');
});

document.getElementById('loginForm').addEventListener('submit', (e)=>{
  e.preventDefault();

  const identity = document.getElementById('identity').value.trim();
  const password = pw.value;

  if(!identity || !password){
    toast('Vui lòng nhập đủ thông tin');
    return;
  }

  // Demo flow: anh thay bằng call API BE /auth/login
  toast('Đăng nhập thành công (demo)');

  // demo redirect (tuỳ role anh xử lý sau)
  setTimeout(()=>{ window.location.href = './home.html'; }, 500);
});