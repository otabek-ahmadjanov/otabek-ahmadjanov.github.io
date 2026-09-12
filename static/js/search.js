(function () {
  var input = document.getElementById('search-input');
  var status = document.getElementById('search-status');
  var results = document.getElementById('search-results');
  if (!input || !status || !results) return;

  var labels = status.dataset;
  var index = null;
  var pending = null;

  function normalize(value) {
    return (value || '').toLowerCase();
  }

  function score(entry, query) {
    var total = 0;
    if (normalize(entry.title).indexOf(query) !== -1) total += 3;
    if (normalize(entry.summary).indexOf(query) !== -1) total += 1;
    entry.tags.forEach(function (tag) {
      if (normalize(tag).indexOf(query) !== -1) total += 2;
    });
    entry.headings.forEach(function (heading) {
      if (normalize(heading).indexOf(query) !== -1) total += 1.5;
    });
    return total;
  }

  function card(entry) {
    var item = document.createElement('li');
    item.className = 'post-card';

    var heading = document.createElement('h2');
    var link = document.createElement('a');
    link.href = entry.url;
    link.textContent = entry.title;
    heading.appendChild(link);
    item.appendChild(heading);

    if (entry.summary) {
      var summary = document.createElement('p');
      summary.className = 'summary';
      summary.textContent = entry.summary;
      item.appendChild(summary);
    }

    if (entry.tags.length) {
      var tags = document.createElement('ul');
      tags.className = 'tag-list';
      entry.tags.forEach(function (tag) {
        var cell = document.createElement('li');
        var badge = document.createElement('span');
        badge.className = 'tag';
        badge.textContent = tag;
        cell.appendChild(badge);
        tags.appendChild(cell);
      });
      item.appendChild(tags);
    }
    return item;
  }

  function show(query) {
    results.replaceChildren();
    if (!query) {
      status.textContent = labels.hint;
      return;
    }
    var matched = index
      .map(function (entry) { return { entry: entry, score: score(entry, query) }; })
      .filter(function (hit) { return hit.score > 0; })
      .sort(function (a, b) { return b.score - a.score; });

    status.textContent = matched.length
      ? labels.results.replace('{0}', matched.length)
      : labels.empty;
    matched.forEach(function (hit) { results.appendChild(card(hit.entry)); });
  }

  function search() {
    var query = normalize(input.value.trim());
    if (index) {
      show(query);
      return;
    }
    if (!pending) {
      pending = fetch(input.dataset.index)
        .then(function (response) { return response.json(); })
        .then(function (loaded) { index = loaded; });
    }
    pending.then(function () { show(normalize(input.value.trim())); });
  }

  input.addEventListener('input', search);
})();
