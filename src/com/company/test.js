var list, c, i;

function listsGetSortCompare(type, direction) {
  var compareFuncs = {
    'NUMERIC': function(a, b) {
        return Number(a) - Number(b); },
    'TEXT': function(a, b) {
        return a.toString() > b.toString() ? 1 : -1; },
    'IGNORE_CASE': function(a, b) {
        return a.toString().toLowerCase() > b.toString().toLowerCase() ? 1 : -1; },
  };
  var compare = compareFuncs[type];
  return function(a, b) { return compare(a, b) * direction; };
}

// Describe this function...
function apply(list) {
  c = '';
  list = list.slice().sort(listsGetSortCompare("TEXT", 1));
  for (var i_index in list) {
    i = list[i_index];
    if (!i.length) {
      continue;
    }
    c = String(c) + String(i);
  }
  return c.split('').reverse().join('');
}

print(apply(['2','c','d1','','f','6h']));
