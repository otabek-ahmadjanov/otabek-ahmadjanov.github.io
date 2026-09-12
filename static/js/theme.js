document.getElementById('theme-toggle')?.addEventListener('click', () => {
  const root = document.documentElement;
  const next = root.dataset.theme === 'dark' ? 'light' : 'dark';
  root.dataset.theme = next;
  localStorage.setItem('theme', next);
});

(function () {
  const more = document.querySelector('.nav-more');
  const toggle = more?.querySelector('.nav-more-toggle');
  if (!more || !toggle) return;

  const setOpen = (open) => {
    more.classList.toggle('open', open);
    toggle.setAttribute('aria-expanded', String(open));
  };

  toggle.addEventListener('click', (e) => {
    e.stopPropagation();
    setOpen(!more.classList.contains('open'));
  });

  document.addEventListener('click', (e) => {
    if (!more.contains(e.target)) setOpen(false);
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') setOpen(false);
  });
})();
