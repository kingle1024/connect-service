/**
 * Connect 백오피스.
 *
 * 백엔드와 같은 오리진에서 서빙되므로 별도 CORS 설정 없이 상대 경로로 호출한다.
 * 관리자 토큰은 sessionStorage 에 두어 탭을 닫으면 사라지게 한다.
 */
(function () {
  'use strict';

  var TOKEN_KEY = 'connect.admin.accessToken';
  var PAGE_SIZE = 20;

  var state = {
    categories: [],          // 카테고리 관리 목록 (비활성 포함)
    boardPage: 0,
    userPage: 0,
    inquiryPage: 0,
    activeTab: 'categories'
  };

  // 문의 상태 코드 -> 화면 표기. 서버 InquiryStatus 와 순서를 맞춘다.
  var INQUIRY_STATUS = {
    RECEIVED: { label: '접수', badge: 'badge-wait' },
    IN_PROGRESS: { label: '확인 중', badge: 'badge-progress' },
    ANSWERED: { label: '답변 완료', badge: 'badge-on' },
    REJECTED: { label: '반영 불가', badge: 'badge-off' }
  };

  // ---------------------------------------------------------------- 유틸

  function $(id) { return document.getElementById(id); }
  function token() { return sessionStorage.getItem(TOKEN_KEY); }

  function escapeHtml(value) {
    if (value === null || value === undefined) { return ''; }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // '2026-07-28T10:00:00' -> '2026-07-28'
  function formatDate(iso) {
    return iso ? String(iso).slice(0, 10) : '-';
  }

  // datetime-local 입력에 넣을 수 있는 'yyyy-MM-ddTHH:mm' 형태로 자른다.
  function toLocalInput(iso) {
    return iso ? String(iso).slice(0, 16) : '';
  }

  var toastTimer = null;
  function toast(message, isError) {
    var el = $('toast');
    el.textContent = message;
    el.className = 'toast' + (isError ? ' toast-error' : '');
    if (toastTimer) { clearTimeout(toastTimer); }
    toastTimer = setTimeout(function () { el.classList.add('hidden'); }, 3200);
  }

  /**
   * 관리자 API 호출. 401/403 이면 세션이 끝난 것으로 보고 로그인 화면으로 되돌린다.
   * 서버는 성공 시 ApiResponse(JSON), 예외 시 평문 메시지를 보낼 수 있어 둘 다 처리한다.
   */
  function api(path, options) {
    var opts = options || {};
    var headers = { 'Accept': 'application/json' };
    if (opts.body) { headers['Content-Type'] = 'application/json'; }
    var accessToken = token();
    if (accessToken) { headers['Authorization'] = 'Bearer ' + accessToken; }

    return fetch(path, {
      method: opts.method || 'GET',
      headers: headers,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    }).then(function (res) {
      return res.text().then(function (text) {
        var payload = null;
        if (text) {
          try { payload = JSON.parse(text); } catch (e) { payload = text; }
        }

        if (res.status === 401 || res.status === 403) {
          if (!opts.allowAuthError) {
            showLogin('세션이 만료되었거나 권한이 없습니다. 다시 로그인해주세요.');
          }
          throw new Error(messageOf(payload) || '권한이 없습니다.');
        }
        if (!res.ok) {
          throw new Error(messageOf(payload) || ('요청 실패 (' + res.status + ')'));
        }
        // ApiResponse 래핑이면 data 를, 아니면 본문 자체를 돌려준다.
        if (payload && typeof payload === 'object' && 'success' in payload) {
          if (payload.success === false) { throw new Error(payload.message || '요청 실패'); }
          return payload.data;
        }
        return payload;
      });
    });
  }

  function messageOf(payload) {
    if (!payload) { return ''; }
    if (typeof payload === 'string') { return payload; }
    return payload.message || '';
  }

  function fail(err) {
    toast(err && err.message ? err.message : '알 수 없는 오류가 발생했습니다.', true);
  }

  // ---------------------------------------------------------------- 로그인

  function showLogin(message) {
    sessionStorage.removeItem(TOKEN_KEY);
    $('app-view').classList.add('hidden');
    $('login-view').classList.remove('hidden');
    $('login-error').textContent = message || '';
    $('login-password').value = '';
  }

  function showApp(admin) {
    $('login-view').classList.add('hidden');
    $('app-view').classList.remove('hidden');
    $('current-admin').textContent = admin.name + ' (' + admin.userId + ')';
    refreshInquiryBadge();
    selectTab(state.activeTab);
  }

  $('login-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var errorEl = $('login-error');
    errorEl.textContent = '';

    var body = {
      userId: $('login-userId').value.trim(),
      password: $('login-password').value
    };

    api('/api/admin/auth/login', { method: 'POST', body: body, allowAuthError: true })
      .then(function (auth) {
        sessionStorage.setItem(TOKEN_KEY, auth.accessToken);
        return api('/api/admin/auth/me', { allowAuthError: true });
      })
      .then(showApp)
      .catch(function (err) {
        sessionStorage.removeItem(TOKEN_KEY);
        errorEl.textContent = err.message;
      });
  });

  $('logout-btn').addEventListener('click', function () {
    showLogin('로그아웃되었습니다.');
  });

  // ---------------------------------------------------------------- 탭

  function selectTab(tab) {
    state.activeTab = tab;
    var buttons = document.querySelectorAll('.nav-item');
    for (var i = 0; i < buttons.length; i++) {
      buttons[i].classList.toggle('active', buttons[i].dataset.tab === tab);
    }
    ['categories', 'boards', 'users', 'inquiries'].forEach(function (name) {
      $('tab-' + name).classList.toggle('hidden', name !== tab);
    });

    if (tab === 'categories') { loadCategories(); }
    if (tab === 'boards') { loadCategories().then(loadBoards).catch(fail); }
    if (tab === 'users') { loadUsers(); }
    if (tab === 'inquiries') { loadInquiries(); }
  }

  document.querySelectorAll('.nav-item').forEach(function (btn) {
    btn.addEventListener('click', function () { selectTab(btn.dataset.tab); });
  });

  // ---------------------------------------------------------------- 카테고리 관리

  function loadCategories() {
    return api('/api/admin/categories').then(function (categories) {
      state.categories = categories || [];
      renderCategories();
      renderCategoryOptions();
      return state.categories;
    }).catch(function (err) { fail(err); throw err; });
  }

  function renderCategories() {
    var tbody = $('category-rows');
    if (!state.categories.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="empty-row">등록된 카테고리가 없습니다. 위에서 추가해주세요.</td></tr>';
      return;
    }

    tbody.innerHTML = state.categories.map(function (c) {
      return '<tr data-cat-id="' + c.id + '">' +
        '<td class="drag-handle" draggable="true" title="끌어서 순서 변경">⣿</td>' +
        '<td>' + escapeHtml(c.name) + '</td>' +
        '<td><span class="badge ' + (c.active ? 'badge-on' : 'badge-off') + '">' +
          (c.active ? '활성' : '비활성') + '</span></td>' +
        '<td>' + c.boardCount + '</td>' +
        '<td><div class="actions">' +
          '<button type="button" class="btn btn-sm" data-cat-edit="' + c.id + '">수정</button>' +
          '<button type="button" class="btn btn-sm" data-cat-toggle="' + c.id + '">' +
            (c.active ? '비활성화' : '활성화') + '</button>' +
          '<button type="button" class="btn btn-sm btn-danger" data-cat-delete="' + c.id + '">삭제</button>' +
        '</div></td>' +
      '</tr>';
    }).join('');
  }

  // ---------------------------------------------------------------- 카테고리 순서 드래그

  var dragRow = null;

  $('category-rows').addEventListener('dragstart', function (e) {
    var handle = e.target.closest('.drag-handle');
    if (!handle) {
      e.preventDefault();
      return;
    }
    dragRow = handle.closest('tr');
    dragRow.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
    // Firefox 는 데이터가 없으면 드래그를 시작하지 않는다.
    e.dataTransfer.setData('text/plain', dragRow.dataset.catId);
  });

  $('category-rows').addEventListener('dragover', function (e) {
    if (!dragRow) { return; }
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';

    var overRow = e.target.closest('tr');
    if (!overRow || overRow === dragRow || !overRow.dataset.catId) { return; }

    // 커서가 대상 행의 아래쪽 절반에 있으면 그 뒤로, 위쪽이면 그 앞으로 옮긴다.
    var rect = overRow.getBoundingClientRect();
    var placeAfter = (e.clientY - rect.top) > rect.height / 2;
    this.insertBefore(dragRow, placeAfter ? overRow.nextSibling : overRow);
  });

  // drop 에서 preventDefault 를 하지 않으면 브라우저가 드래그를 취소로 처리한다.
  $('category-rows').addEventListener('drop', function (e) { e.preventDefault(); });

  $('category-rows').addEventListener('dragend', function () {
    if (!dragRow) { return; }
    dragRow.classList.remove('dragging');
    dragRow = null;
    persistCategoryOrder();
  });

  function persistCategoryOrder() {
    var rows = $('category-rows').querySelectorAll('tr[data-cat-id]');
    var ids = Array.prototype.map.call(rows, function (tr) { return Number(tr.dataset.catId); });
    var before = state.categories.map(function (c) { return c.id; });

    // 순서가 그대로면 서버를 호출하지 않는다.
    if (ids.join(',') === before.join(',')) { return; }

    api('/api/admin/categories/order', { method: 'PUT', body: { ids: ids } })
      .then(function () {
        toast('순서를 변경했습니다.');
        return loadCategories();
      })
      .catch(function (err) {
        fail(err);
        return loadCategories(); // 실패 시 서버 기준 순서로 되돌린다.
      });
  }

  // 게시물 필터/수정 화면의 카테고리 select 를 채운다.
  function renderCategoryOptions() {
    var filter = $('board-category');
    var keptFilter = filter.value;
    filter.innerHTML = '<option value="">전체 카테고리</option>' + state.categories.map(function (c) {
      return '<option value="' + escapeHtml(c.name) + '">' + escapeHtml(c.name) + '</option>';
    }).join('');
    filter.value = keptFilter;

    // 수정 모달에서는 활성 카테고리만 고를 수 있다 (서버도 같은 규칙으로 검증한다).
    $('board-edit-category').innerHTML = state.categories.filter(function (c) { return c.active; })
      .map(function (c) {
        return '<option value="' + escapeHtml(c.name) + '">' + escapeHtml(c.name) + '</option>';
      }).join('');
  }

  function findCategory(id) {
    return state.categories.filter(function (c) { return String(c.id) === String(id); })[0];
  }

  $('category-create-form').addEventListener('submit', function (e) {
    e.preventDefault();
    // sortOrder 를 보내지 않으면 서버가 목록 맨 뒤에 붙인다.
    var body = {
      name: $('category-new-name').value.trim(),
      active: true
    };
    api('/api/admin/categories', { method: 'POST', body: body })
      .then(function () {
        $('category-new-name').value = '';
        toast('카테고리를 추가했습니다.');
        return loadCategories();
      })
      .catch(fail);
  });

  $('category-rows').addEventListener('click', function (e) {
    var btn = e.target.closest('button');
    if (!btn) { return; }

    if (btn.dataset.catEdit) {
      openCategoryModal(findCategory(btn.dataset.catEdit));
    } else if (btn.dataset.catToggle) {
      var category = findCategory(btn.dataset.catToggle);
      saveCategory(category.id, {
        name: category.name,
        active: !category.active
      }).catch(fail);
    } else if (btn.dataset.catDelete) {
      var target = findCategory(btn.dataset.catDelete);
      if (!confirm('"' + target.name + '" 카테고리를 삭제할까요?')) { return; }
      api('/api/admin/categories/' + target.id, { method: 'DELETE' })
        .then(function () { toast('카테고리를 삭제했습니다.'); return loadCategories(); })
        .catch(fail);
    }
  });

  function saveCategory(id, body) {
    return api('/api/admin/categories/' + id, { method: 'PUT', body: body })
      .then(function () { toast('카테고리를 수정했습니다.'); return loadCategories(); });
  }

  function openCategoryModal(category) {
    $('category-edit-id').value = category.id;
    $('category-edit-name').value = category.name;
    $('category-edit-active').checked = category.active;
    $('category-edit-error').textContent = '';
    $('category-modal').classList.remove('hidden');
  }

  $('category-edit-form').addEventListener('submit', function (e) {
    e.preventDefault();
    // sortOrder 를 보내지 않으면 서버가 기존 순서를 유지한다 (순서는 드래그로만 바꾼다).
    var body = {
      name: $('category-edit-name').value.trim(),
      active: $('category-edit-active').checked
    };
    saveCategory($('category-edit-id').value, body)
      .then(function () { closeModals(); })
      .catch(function (err) { $('category-edit-error').textContent = err.message; });
  });

  // ---------------------------------------------------------------- 게시물 관리

  function boardQuery() {
    var params = new URLSearchParams();
    var keyword = $('board-keyword').value.trim();
    if (keyword) { params.set('keyword', keyword); }
    if ($('board-category').value) { params.set('category', $('board-category').value); }
    if ($('board-deleted').value) { params.set('deleted', $('board-deleted').value); }
    params.set('page', state.boardPage);
    params.set('size', PAGE_SIZE);
    return params.toString();
  }

  function loadBoards() {
    return api('/api/admin/boards?' + boardQuery()).then(function (page) {
      renderBoards(page);
    }).catch(fail);
  }

  function renderBoards(page) {
    var tbody = $('board-rows');
    if (!page.content.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-row">조건에 맞는 게시물이 없습니다.</td></tr>';
    } else {
      tbody.innerHTML = page.content.map(function (b) {
        return '<tr>' +
          '<td><div class="cell-title" title="' + escapeHtml(b.title) + '">' + escapeHtml(b.title) + '</div></td>' +
          '<td>' + escapeHtml(b.category) + '</td>' +
          '<td>' + escapeHtml(b.userName) + '<br /><span class="badge">' + escapeHtml(b.userId) + '</span></td>' +
          '<td>' + formatDate(b.deadlineDts) + '</td>' +
          '<td>' + b.currentParticipants + ' / ' + b.maxCapacity + '</td>' +
          '<td><span class="badge ' + (b.deleted ? 'badge-off' : 'badge-on') + '">' +
            (b.deleted ? '삭제됨' : '정상') + '</span></td>' +
          '<td><div class="actions">' +
            '<button type="button" class="btn btn-sm" data-board-edit="' + b.id + '">수정</button>' +
            (b.deleted
              ? '<button type="button" class="btn btn-sm" data-board-restore="' + b.id + '">복원</button>'
              : '<button type="button" class="btn btn-sm btn-danger" data-board-delete="' + b.id + '">삭제</button>') +
          '</div></td>' +
        '</tr>';
      }).join('');
    }
    renderPager($('board-pager'), page, function (next) {
      state.boardPage = next;
      loadBoards();
    });
  }

  $('board-filter-form').addEventListener('submit', function (e) {
    e.preventDefault();
    state.boardPage = 0;
    loadBoards();
  });

  $('board-rows').addEventListener('click', function (e) {
    var btn = e.target.closest('button');
    if (!btn) { return; }

    if (btn.dataset.boardEdit) {
      api('/api/admin/boards/' + btn.dataset.boardEdit).then(openBoardModal).catch(fail);
    } else if (btn.dataset.boardDelete) {
      // 화면에 ID를 노출하지 않으므로 제목으로 확인을 받는다.
      var title = btn.closest('tr').querySelector('.cell-title').textContent;
      if (!confirm('"' + title + '" 게시물을 삭제할까요? (복원할 수 있습니다)')) { return; }
      api('/api/admin/boards/' + btn.dataset.boardDelete, { method: 'DELETE' })
        .then(function () { toast('게시물을 삭제했습니다.'); return loadBoards(); })
        .catch(fail);
    } else if (btn.dataset.boardRestore) {
      api('/api/admin/boards/' + btn.dataset.boardRestore + '/restore', { method: 'POST' })
        .then(function () { toast('게시물을 복원했습니다.'); return loadBoards(); })
        .catch(fail);
    }
  });

  function openBoardModal(board) {
    $('board-edit-id').value = board.id;
    $('board-edit-title').value = board.title;
    $('board-edit-content').value = board.content;
    $('board-edit-destination').value = board.destination;
    $('board-edit-capacity').value = board.maxCapacity;
    $('board-edit-deadline').value = toLocalInput(board.deadlineDts);
    $('board-edit-error').textContent = '';

    // 현재 카테고리가 비활성이라 select 에 없을 수 있으므로 그대로 유지할 수 있게 추가해준다.
    var select = $('board-edit-category');
    var exists = Array.prototype.some.call(select.options, function (o) { return o.value === board.category; });
    if (!exists && board.category) {
      var option = document.createElement('option');
      option.value = board.category;
      option.textContent = board.category + ' (비활성)';
      select.appendChild(option);
    }
    select.value = board.category;

    $('board-modal').classList.remove('hidden');
  }

  $('board-edit-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var body = {
      title: $('board-edit-title').value.trim(),
      content: $('board-edit-content').value.trim(),
      category: $('board-edit-category').value,
      destination: $('board-edit-destination').value.trim(),
      maxCapacity: Number($('board-edit-capacity').value),
      deadlineDts: $('board-edit-deadline').value
    };
    api('/api/admin/boards/' + $('board-edit-id').value, { method: 'PUT', body: body })
      .then(function () {
        closeModals();
        toast('게시물을 수정했습니다.');
        return loadBoards();
      })
      .catch(function (err) { $('board-edit-error').textContent = err.message; });
  });

  // ---------------------------------------------------------------- 회원 관리

  function userQuery() {
    var params = new URLSearchParams();
    var keyword = $('user-keyword').value.trim();
    if (keyword) { params.set('keyword', keyword); }
    if ($('user-role').value) { params.set('role', $('user-role').value); }
    if ($('user-enabled').value) { params.set('enabled', $('user-enabled').value); }
    params.set('page', state.userPage);
    params.set('size', PAGE_SIZE);
    return params.toString();
  }

  function loadUsers() {
    return api('/api/admin/users?' + userQuery()).then(function (page) {
      renderUsers(page);
    }).catch(fail);
  }

  function renderUsers(page) {
    var tbody = $('user-rows');
    if (!page.content.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-row">조건에 맞는 회원이 없습니다.</td></tr>';
    } else {
      tbody.innerHTML = page.content.map(function (u) {
        var roles = u.roles.map(function (r) {
          var cls = r === 'ROLE_ADMIN' ? 'badge badge-admin' : 'badge';
          return '<span class="' + cls + '">' + escapeHtml(r.replace('ROLE_', '')) + '</span>';
        }).join('');

        return '<tr>' +
          '<td>' + escapeHtml(u.userId) + '</td>' +
          '<td>' + escapeHtml(u.name) + '</td>' +
          '<td>' + escapeHtml(u.email) + '</td>' +
          '<td><div class="role-list">' + roles + '</div></td>' +
          '<td><span class="badge ' + (u.enabled ? 'badge-on' : 'badge-off') + '">' +
            (u.enabled ? '활성' : '정지') + '</span></td>' +
          '<td><div class="actions">' +
            '<button type="button" class="btn btn-sm" data-user-roles="' + escapeHtml(u.userId) + '">권한</button>' +
            '<button type="button" class="btn btn-sm" data-user-password="' + escapeHtml(u.userId) + '">비밀번호</button>' +
            (u.enabled
              ? '<button type="button" class="btn btn-sm btn-danger" data-user-disable="' + escapeHtml(u.userId) + '">정지</button>'
              : '<button type="button" class="btn btn-sm" data-user-enable="' + escapeHtml(u.userId) + '">활성화</button>') +
          '</div></td>' +
        '</tr>';
      }).join('');
    }
    renderPager($('user-pager'), page, function (next) {
      state.userPage = next;
      loadUsers();
    });
  }

  $('user-filter-form').addEventListener('submit', function (e) {
    e.preventDefault();
    state.userPage = 0;
    loadUsers();
  });

  $('user-rows').addEventListener('click', function (e) {
    var btn = e.target.closest('button');
    if (!btn) { return; }

    if (btn.dataset.userRoles) {
      api('/api/admin/users/' + encodeURIComponent(btn.dataset.userRoles))
        .then(openUserModal).catch(fail);
    } else if (btn.dataset.userPassword) {
      var newPassword = prompt(btn.dataset.userPassword + ' 회원의 새 비밀번호를 입력하세요. (4자 이상)');
      if (!newPassword) { return; }
      api('/api/admin/users/' + encodeURIComponent(btn.dataset.userPassword) + '/password', {
        method: 'PUT',
        body: { newPassword: newPassword }
      })
        .then(function () { toast('비밀번호를 변경했습니다. 기존 로그인 세션은 해제됩니다.'); })
        .catch(fail);
    } else if (btn.dataset.userDisable) {
      if (!confirm(btn.dataset.userDisable + ' 회원을 정지할까요? 즉시 로그인이 차단됩니다.')) { return; }
      setUserEnabled(btn.dataset.userDisable, false);
    } else if (btn.dataset.userEnable) {
      setUserEnabled(btn.dataset.userEnable, true);
    }
  });

  function setUserEnabled(userId, enabled) {
    api('/api/admin/users/' + encodeURIComponent(userId) + '/enabled', {
      method: 'PUT',
      body: { enabled: enabled }
    })
      .then(function () {
        toast(enabled ? '계정을 활성화했습니다.' : '계정을 정지했습니다.');
        return loadUsers();
      })
      .catch(fail);
  }

  function openUserModal(user) {
    $('user-edit-id').value = user.userId;
    $('user-edit-target').textContent = user.name + ' (' + user.userId + ')';
    document.querySelectorAll('.user-role-check').forEach(function (input) {
      input.checked = user.roles.indexOf(input.value) !== -1;
    });
    $('user-edit-error').textContent = '';
    $('user-modal').classList.remove('hidden');
  }

  $('user-edit-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var roles = [];
    document.querySelectorAll('.user-role-check').forEach(function (input) {
      if (input.checked) { roles.push(input.value); }
    });

    api('/api/admin/users/' + encodeURIComponent($('user-edit-id').value) + '/roles', {
      method: 'PUT',
      body: { roles: roles }
    })
      .then(function () {
        closeModals();
        toast('권한을 변경했습니다.');
        return loadUsers();
      })
      .catch(function (err) { $('user-edit-error').textContent = err.message; });
  });

  // ---------------------------------------------------------------- 문의 관리

  function inquiryQuery() {
    var params = new URLSearchParams();
    var keyword = $('inquiry-keyword').value.trim();
    if (keyword) { params.set('keyword', keyword); }
    if ($('inquiry-type').value) { params.set('type', $('inquiry-type').value); }
    if ($('inquiry-status').value) { params.set('status', $('inquiry-status').value); }
    if ($('inquiry-deleted').value) { params.set('deleted', $('inquiry-deleted').value); }
    params.set('page', state.inquiryPage);
    params.set('size', PAGE_SIZE);
    return params.toString();
  }

  function loadInquiries() {
    return api('/api/admin/inquiries?' + inquiryQuery()).then(function (page) {
      renderInquiries(page);
      refreshInquiryBadge();
    }).catch(fail);
  }

  // 사이드바에 미답변(접수/확인 중) 건수를 표시한다. 실패해도 화면은 그대로 둔다.
  function refreshInquiryBadge() {
    api('/api/admin/inquiries/pending-count').then(function (count) {
      var badge = $('inquiry-badge');
      badge.textContent = count;
      badge.classList.toggle('hidden', !count);
    }).catch(function () { /* 배지는 부가 정보라 조용히 넘긴다 */ });
  }

  function statusBadge(status) {
    var meta = INQUIRY_STATUS[status] || { label: status, badge: '' };
    return '<span class="badge ' + meta.badge + '">' + escapeHtml(meta.label) + '</span>';
  }

  // 회원은 로그인 아이디를, 비회원은 답변받을 메일 주소(안 남겼으면 '메일 없음')를 보여준다.
  function writerBadge(q) {
    if (!q.guest) {
      return '<span class="badge">' + escapeHtml(q.userId || '-') + '</span>';
    }
    return '<span class="badge badge-off">비로그인</span> ' +
      '<span class="badge">' + escapeHtml(q.guestEmail || '메일 없음') + '</span>';
  }

  function renderInquiries(page) {
    var tbody = $('inquiry-rows');
    if (!page.content.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-row">조건에 맞는 문의가 없습니다.</td></tr>';
    } else {
      tbody.innerHTML = page.content.map(function (q) {
        return '<tr>' +
          '<td>' + formatDate(q.insertDts) + '</td>' +
          '<td><span class="badge">' + escapeHtml(q.typeLabel) + '</span></td>' +
          '<td><div class="cell-title" title="' + escapeHtml(q.title) + '">' + escapeHtml(q.title) + '</div></td>' +
          '<td>' + escapeHtml(q.userName) + '<br />' + writerBadge(q) + '</td>' +
          '<td>' + statusBadge(q.status) +
            (q.deleted ? ' <span class="badge badge-off">숨김</span>' : '') + '</td>' +
          '<td><div class="actions">' +
            '<button type="button" class="btn btn-sm btn-primary" data-inquiry-answer="' + q.id + '">' +
              (q.answer ? '답변 수정' : '답변') + '</button>' +
            (q.status === 'RECEIVED'
              ? '<button type="button" class="btn btn-sm" data-inquiry-progress="' + q.id + '">확인 중</button>'
              : '') +
            (q.deleted
              ? '<button type="button" class="btn btn-sm" data-inquiry-restore="' + q.id + '">복원</button>'
              : '<button type="button" class="btn btn-sm btn-danger" data-inquiry-delete="' + q.id + '">숨김</button>') +
          '</div></td>' +
        '</tr>';
      }).join('');
    }
    renderPager($('inquiry-pager'), page, function (next) {
      state.inquiryPage = next;
      loadInquiries();
    });
  }

  $('inquiry-filter-form').addEventListener('submit', function (e) {
    e.preventDefault();
    state.inquiryPage = 0;
    loadInquiries();
  });

  $('inquiry-rows').addEventListener('click', function (e) {
    var btn = e.target.closest('button');
    if (!btn) { return; }

    if (btn.dataset.inquiryAnswer) {
      api('/api/admin/inquiries/' + btn.dataset.inquiryAnswer).then(openInquiryModal).catch(fail);
    } else if (btn.dataset.inquiryProgress) {
      api('/api/admin/inquiries/' + btn.dataset.inquiryProgress + '/status', {
        method: 'PUT',
        body: { status: 'IN_PROGRESS' }
      })
        .then(function () { toast('확인 중으로 변경했습니다.'); return loadInquiries(); })
        .catch(fail);
    } else if (btn.dataset.inquiryDelete) {
      if (!confirm('이 문의를 숨길까요? 작성자의 문의 내역에서도 보이지 않습니다. (복원할 수 있습니다)')) { return; }
      api('/api/admin/inquiries/' + btn.dataset.inquiryDelete, { method: 'DELETE' })
        .then(function () { toast('문의를 숨겼습니다.'); return loadInquiries(); })
        .catch(fail);
    } else if (btn.dataset.inquiryRestore) {
      api('/api/admin/inquiries/' + btn.dataset.inquiryRestore + '/restore', { method: 'POST' })
        .then(function () { toast('문의를 복원했습니다.'); return loadInquiries(); })
        .catch(fail);
    }
  });

  function openInquiryModal(inquiry) {
    $('inquiry-edit-id').value = inquiry.id;
    $('inquiry-edit-target').textContent =
      '[' + inquiry.typeLabel + '] ' + inquiry.title +
      ' · ' + inquiry.userName +
      ' (' + (inquiry.guest ? (inquiry.guestEmail || '비로그인 · 메일 없음') : inquiry.userId) + ')' +
      ' · ' + formatDate(inquiry.insertDts);
    $('inquiry-edit-content').textContent = inquiry.content;
    $('inquiry-edit-answer').value = inquiry.answer || '';
    // 답변을 저장할 때의 기본 상태는 '답변 완료'. 반영 불가 등으로 남기고 싶으면 바꿀 수 있다.
    $('inquiry-edit-status').value = inquiry.answer ? inquiry.status : 'ANSWERED';
    $('inquiry-edit-error').textContent = '';
    $('inquiry-modal').classList.remove('hidden');
  }

  $('inquiry-answer-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var body = {
      answer: $('inquiry-edit-answer').value.trim(),
      status: $('inquiry-edit-status').value
    };
    api('/api/admin/inquiries/' + $('inquiry-edit-id').value + '/answer', { method: 'PUT', body: body })
      .then(function () {
        closeModals();
        toast('답변을 등록했습니다.');
        return loadInquiries();
      })
      .catch(function (err) { $('inquiry-edit-error').textContent = err.message; });
  });

  // ---------------------------------------------------------------- 공통 UI

  function renderPager(container, page, onSelect) {
    if (page.totalElements === 0) {
      container.innerHTML = '';
      return;
    }
    container.innerHTML =
      '<span class="page-info">전체 ' + page.totalElements + '건 · ' +
        (page.page + 1) + ' / ' + Math.max(page.totalPages, 1) + ' 페이지</span>' +
      '<button type="button" class="btn btn-sm" data-page="' + (page.page - 1) + '"' +
        (page.page <= 0 ? ' disabled' : '') + '>이전</button>' +
      '<button type="button" class="btn btn-sm" data-page="' + (page.page + 1) + '"' +
        (page.page + 1 >= page.totalPages ? ' disabled' : '') + '>다음</button>';

    container.querySelectorAll('button[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () { onSelect(Number(btn.dataset.page)); });
    });
  }

  function closeModals() {
    document.querySelectorAll('.modal').forEach(function (m) { m.classList.add('hidden'); });
  }

  document.querySelectorAll('[data-close-modal]').forEach(function (btn) {
    btn.addEventListener('click', closeModals);
  });

  // 배경(오버레이) 클릭으로 닫기. 카드 내부 클릭은 무시한다.
  document.querySelectorAll('.modal').forEach(function (modal) {
    modal.addEventListener('click', function (e) {
      if (e.target === modal) { closeModals(); }
    });
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { closeModals(); }
  });

  // ---------------------------------------------------------------- 시작

  if (token()) {
    // 새로고침: 저장된 토큰이 아직 쓸 수 있는지 확인한다.
    api('/api/admin/auth/me', { allowAuthError: true })
      .then(showApp)
      .catch(function () { showLogin(); });
  } else {
    showLogin();
  }
})();
