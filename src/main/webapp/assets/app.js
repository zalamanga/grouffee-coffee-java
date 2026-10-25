const toggle = document.querySelector('.menu-toggle');
const navigation = document.querySelector('#navigation');
if (toggle && navigation) {
  toggle.hidden = false;
  navigation.classList.add('enhanced');
  toggle.addEventListener('click', () => {
    const open = toggle.getAttribute('aria-expanded') !== 'true';
    toggle.setAttribute('aria-expanded', String(open));
    navigation.classList.toggle('open', open);
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && navigation.classList.contains('open')) {
      toggle.setAttribute('aria-expanded', 'false');
      navigation.classList.remove('open');
      toggle.focus();
    }
  });
}
document.querySelectorAll('form[method="post"]').forEach(form => {
  form.addEventListener('submit', () => {
    if (!form.checkValidity()) return;
    const status = document.querySelector('.submit-status');
    if (status) { status.textContent = 'Menyimpan perubahan…'; status.hidden = false; }
    form.querySelectorAll('button').forEach(button => { button.disabled = true; });
    form.setAttribute('aria-busy', 'true');
  });
});
document.addEventListener('click', event => {
  const link = event.target.closest('a[href]');
  if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
  const destination = new URL(link.href, location.href);
  if (destination.origin !== location.origin || destination.pathname === location.pathname && destination.search === location.search) return;
  const status = document.querySelector('.submit-status');
  if (status) { status.textContent = 'Memuat halaman…'; status.hidden = false; }
});
window.addEventListener('pageshow', () => {
  const status = document.querySelector('.submit-status');
  if (status) status.hidden = true;
  document.querySelectorAll('form[aria-busy="true"]').forEach(form => {
    form.removeAttribute('aria-busy');
    form.querySelectorAll('button').forEach(button => { button.disabled = false; });
  });
});
document.querySelector('.error')?.focus();
