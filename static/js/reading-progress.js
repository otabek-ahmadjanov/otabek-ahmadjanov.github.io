(function () {
  const bar = document.querySelector('[data-reading-progress]');
  if (!bar) return;

  const article = document.querySelector('article');
  let ticking = false;

  function progress() {
    if (!article) {
      const el = document.documentElement;
      const max = el.scrollHeight - el.clientHeight;
      return max > 0 ? el.scrollTop / max : 0;
    }
    const rect = article.getBoundingClientRect();
    const distance = rect.height - window.innerHeight;
    if (distance <= 0) return rect.top <= 0 ? 1 : 0;
    return Math.min(1, Math.max(0, -rect.top / distance));
  }

  function update() {
    bar.style.transform = 'scaleX(' + progress() + ')';
    ticking = false;
  }

  function onScroll() {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(update);
  }

  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', onScroll, { passive: true });
  update();
})();
