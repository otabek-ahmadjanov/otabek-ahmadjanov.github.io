(function () {
  const btn = document.querySelector('[data-copy-link]');
  if (!btn) return;

  const label = btn.querySelector('.copy-link-label');
  const original = label ? label.textContent : '';
  const copiedText = btn.dataset.copiedLabel || 'copied';
  let resetTimer;

  async function copy(text) {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    document.body.removeChild(area);
  }

  btn.addEventListener('click', async () => {
    try {
      await copy(window.location.href);
      btn.classList.add('copied');
      if (label) label.textContent = copiedText;
      clearTimeout(resetTimer);
      resetTimer = setTimeout(() => {
        btn.classList.remove('copied');
        if (label) label.textContent = original;
      }, 2000);
    } catch (e) {
      /* clipboard unavailable */
    }
  });
})();
