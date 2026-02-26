document.getElementById('y').textContent = new Date().getFullYear();

document.addEventListener('click', (e)=>{
  const btn = e.target.closest('[data-toggle]');
  if(!btn) return;
  const id = btn.getAttribute('data-toggle');
  const input = document.getElementById(id);
  input.type = (input.type === 'password') ? 'text' : 'password';
  toast(input.type === 'password' ? 'Đã ẩn mật khẩu' : 'Đã hiện mật khẩu');
});

document.getElementById('registerForm').addEventListener('submit', (e)=>{
  e.preventDefault();

  const pw1 = document.getElementById('pw1').value;
  const pw2 = document.getElementById('pw2').value;

  if(pw1.length < 8){
    toast('Mật khẩu tối thiểu 8 ký tự');
    return;
  }
  if(pw1 !== pw2){
    toast('Mật khẩu nhập lại không khớp');
    return;
  }
  if(!document.getElementById('agree').checked){
    toast('Vui lòng đồng ý điều khoản');
    return;
  }

  // Demo flow: anh thay bằng call API BE /auth/register
  toast('Đăng ký thành công (demo)');
  setTimeout(()=>{ window.location.href = './login.html'; }, 600);
});