import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  BarChart3,
  Boxes,
  Check,
  ClipboardList,
  Edit3,
  History,
  LogOut,
  Plus,
  RefreshCw,
  Save,
  Trash2,
  X,
} from 'lucide-react';
import './styles.css';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';
const POLL_INTERVAL_MS = 15000;

const emptySupplier = {
  fruitName: '',
  purchasePrice: '',
  stockKg: '',
  supplierName: '',
  deliveryDays: '',
};

const emptyOrder = {
  restaurant: '',
  fruitName: '',
  quantityKg: '',
  orderDate: '',
  deliveryDate: '',
};

function authHeader(credentials) {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`;
}

async function request(path, credentials, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      Authorization: authHeader(credentials),
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message ?? `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function can(user, permission) {
  return user?.permissions?.includes(permission);
}

function money(value) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return new Intl.NumberFormat('ru-KZ', {
    style: 'currency',
    currency: 'KZT',
    maximumFractionDigits: 0,
  }).format(Number(value));
}

function number(value) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return new Intl.NumberFormat('ru-KZ', { maximumFractionDigits: 2 }).format(Number(value));
}

function App() {
  const savedCredentials = useMemo(() => {
    const raw = localStorage.getItem('ksm.credentials');
    return raw ? JSON.parse(raw) : null;
  }, []);

  const [credentials, setCredentials] = useState(savedCredentials);
  const [user, setUser] = useState(null);
  const [activePage, setActivePage] = useState('suppliers');
  const [suppliers, setSuppliers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [finance, setFinance] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [lastSync, setLastSync] = useState(null);

  async function loadAll(currentCredentials = credentials, currentUser = user) {
    if (!currentCredentials) {
      return;
    }
    setError('');
    const nextSuppliers = await request('/suppliers', currentCredentials);
    const nextOrders = await request('/orders', currentCredentials);
    setSuppliers(nextSuppliers);
    setOrders(nextOrders);
    if (currentUser && (can(currentUser, 'finance:own') || can(currentUser, 'finance:company'))) {
      setFinance(await request('/finance', currentCredentials));
    } else {
      setFinance(null);
    }
    if (currentUser && can(currentUser, 'audit:read')) {
      setAuditLogs(await request('/audit', currentCredentials));
    } else {
      setAuditLogs([]);
    }
    setLastSync(new Date());
  }

  async function login(nextCredentials) {
    setLoading(true);
    setError('');
    try {
      const nextUser = await request('/auth/me', nextCredentials);
      localStorage.setItem('ksm.credentials', JSON.stringify(nextCredentials));
      setCredentials(nextCredentials);
      setUser(nextUser);
      setActivePage('suppliers');
      await loadAll(nextCredentials, nextUser);
    } catch (err) {
      setError(err.message.includes('HTTP 401') ? 'Неверный логин или пароль' : `Не удалось подключиться к backend: ${err.message}`);
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem('ksm.credentials');
    setCredentials(null);
    setUser(null);
    setFinance(null);
    setAuditLogs([]);
    setOrders([]);
    setSuppliers([]);
    setActivePage('suppliers');
  }

  useEffect(() => {
    if (!credentials || user) {
      return;
    }
    login(credentials);
  }, []);

  useEffect(() => {
    if (!credentials || !user) {
      return undefined;
    }
    const timer = setInterval(() => {
      loadAll().catch((err) => setError(err.message));
    }, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [credentials, user]);

  if (!user) {
    return <LoginScreen onLogin={login} loading={loading} error={error} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">K</span>
          <div>
            <strong>KSM Fruit</strong>
            <small>{user.roleLabel}</small>
          </div>
        </div>
        <nav className="nav">
          <button className={activePage === 'suppliers' ? 'active' : ''} onClick={() => setActivePage('suppliers')}>
            <Boxes size={18} /> Поставщики
          </button>
          <button className={activePage === 'orders' ? 'active' : ''} onClick={() => setActivePage('orders')}>
            <ClipboardList size={18} /> Заказы
          </button>
          {(can(user, 'finance:own') || can(user, 'finance:company')) && (
            <button className={activePage === 'finance' ? 'active' : ''} onClick={() => setActivePage('finance')}>
              <BarChart3 size={18} /> Финансы
            </button>
          )}
          {can(user, 'audit:read') && (
            <button className={activePage === 'audit' ? 'active' : ''} onClick={() => setActivePage('audit')}>
              <History size={18} /> История
            </button>
          )}
        </nav>
        <button className="ghost-button logout" onClick={logout}>
          <LogOut size={18} /> Выйти
        </button>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Рабочее пространство</p>
            <h1>{pageTitle(activePage)}</h1>
          </div>
          <div className="sync">
            <span>{lastSync ? `Обновлено ${lastSync.toLocaleTimeString('ru-RU')}` : 'Ожидание синхронизации'}</span>
            <button className="icon-button" title="Обновить" onClick={() => loadAll().catch((err) => setError(err.message))}>
              <RefreshCw size={18} />
            </button>
          </div>
        </header>

        {error && <div className="alert">{error}</div>}

        {activePage === 'suppliers' && (
          <SuppliersPage
            suppliers={suppliers}
            credentials={credentials}
            canWrite={can(user, 'suppliers:write')}
            reload={loadAll}
            setError={setError}
          />
        )}
        {activePage === 'orders' && (
          <OrdersPage
            orders={orders}
            credentials={credentials}
            canWrite={can(user, 'orders:write')}
            reload={loadAll}
            setError={setError}
          />
        )}
        {activePage === 'finance' && finance && <FinancePage finance={finance} user={user} />}
        {activePage === 'audit' && <AuditPage logs={auditLogs} />}
      </main>
    </div>
  );
}

function pageTitle(page) {
  return {
    suppliers: 'Поставщики',
    orders: 'Заказы',
    finance: 'Финансы',
    audit: 'История изменений',
  }[page];
}

function LoginScreen({ onLogin, loading, error }) {
  const [form, setForm] = useState({ username: '', password: '' });
  return (
    <div className="login-screen">
      <form
        className="login-panel"
        onSubmit={(event) => {
          event.preventDefault();
          onLogin(form);
        }}
      >
        <div className="brand login-brand">
          <span className="brand-mark">K</span>
          <div>
            <strong>KSM Fruit</strong>
            <small>Авторизация</small>
          </div>
        </div>
        <label>
          Логин
          <input value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} autoFocus />
        </label>
        <label>
          Пароль
          <input
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            type="password"
          />
        </label>
        {error && <div className="alert">{error}</div>}
        <button className="primary-button" disabled={loading}>
          <Check size={18} /> Войти
        </button>
      </form>
    </div>
  );
}

function SuppliersPage({ suppliers, credentials, canWrite, reload, setError }) {
  const [form, setForm] = useState(emptySupplier);
  const [editingId, setEditingId] = useState(null);

  async function save(event) {
    event.preventDefault();
    const payload = {
      fruitName: form.fruitName,
      purchasePrice: Number(form.purchasePrice),
      stockKg: Number(form.stockKg),
      supplierName: form.supplierName,
      deliveryDays: Number(form.deliveryDays),
    };
    try {
      await request(editingId ? `/suppliers/${editingId}` : '/suppliers', credentials, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      });
      setForm(emptySupplier);
      setEditingId(null);
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function remove(id) {
    try {
      await request(`/suppliers/${id}`, credentials, { method: 'DELETE' });
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="workspace">
      {canWrite && (
        <form className="editor-grid" onSubmit={save}>
          <input placeholder="Фрукт" value={form.fruitName} onChange={(e) => setForm({ ...form, fruitName: e.target.value })} required />
          <input placeholder="Поставщик" value={form.supplierName} onChange={(e) => setForm({ ...form, supplierName: e.target.value })} required />
          <input placeholder="Цена закупки" type="number" min="0" step="0.01" value={form.purchasePrice} onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} required />
          <input placeholder="Остаток кг" type="number" min="0" step="0.01" value={form.stockKg} onChange={(e) => setForm({ ...form, stockKg: e.target.value })} required />
          <input placeholder="Срок, дней" type="number" min="0" value={form.deliveryDays} onChange={(e) => setForm({ ...form, deliveryDays: e.target.value })} required />
          <div className="form-actions">
            <button className="primary-button">
              {editingId ? <Save size={18} /> : <Plus size={18} />} {editingId ? 'Сохранить' : 'Добавить'}
            </button>
            {editingId && (
              <button type="button" className="ghost-button" onClick={() => { setEditingId(null); setForm(emptySupplier); }}>
                <X size={18} /> Отмена
              </button>
            )}
          </div>
        </form>
      )}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>№</th>
              <th>Фрукт</th>
              <th>Поставщик</th>
              <th>Цена закупки</th>
              <th>Остаток</th>
              <th>Срок</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {suppliers.map((supplier) => (
              <tr key={supplier.id}>
                <td>{supplier.id}</td>
                <td>{supplier.fruitName}</td>
                <td>{supplier.supplierName}</td>
                <td>{money(supplier.purchasePrice)}</td>
                <td>{number(supplier.stockKg)} кг</td>
                <td>{supplier.deliveryDays} дн.</td>
                {canWrite && (
                  <td className="row-actions">
                    <button className="icon-button" title="Редактировать" onClick={() => { setEditingId(supplier.id); setForm(supplier); }}>
                      <Edit3 size={16} />
                    </button>
                    <button className="icon-button danger" title="Удалить" onClick={() => remove(supplier.id)}>
                      <Trash2 size={16} />
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function OrdersPage({ orders, credentials, canWrite, reload, setError }) {
  const [form, setForm] = useState(emptyOrder);
  const [editingId, setEditingId] = useState(null);

  async function save(event) {
    event.preventDefault();
    const payload = {
      restaurant: form.restaurant,
      fruitName: form.fruitName,
      quantityKg: Number(form.quantityKg),
      orderDate: form.orderDate,
      deliveryDate: form.deliveryDate,
    };
    try {
      await request(editingId ? `/orders/${editingId}` : '/orders', credentials, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      });
      setForm(emptyOrder);
      setEditingId(null);
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function remove(id) {
    try {
      await request(`/orders/${id}`, credentials, { method: 'DELETE' });
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="workspace">
      {canWrite && (
        <form className="editor-grid order-editor" onSubmit={save}>
          <input placeholder="Ресторан" value={form.restaurant} onChange={(e) => setForm({ ...form, restaurant: e.target.value })} required />
          <input placeholder="Фрукт" value={form.fruitName} onChange={(e) => setForm({ ...form, fruitName: e.target.value })} required />
          <input placeholder="Количество кг" type="number" min="0" step="0.01" value={form.quantityKg} onChange={(e) => setForm({ ...form, quantityKg: e.target.value })} required />
          <input type="date" value={form.orderDate} onChange={(e) => setForm({ ...form, orderDate: e.target.value })} required />
          <input type="date" value={form.deliveryDate} onChange={(e) => setForm({ ...form, deliveryDate: e.target.value })} required />
          <div className="form-actions">
            <button className="primary-button">
              {editingId ? <Save size={18} /> : <Plus size={18} />} {editingId ? 'Сохранить' : 'Создать'}
            </button>
            {editingId && (
              <button type="button" className="ghost-button" onClick={() => { setEditingId(null); setForm(emptyOrder); }}>
                <X size={18} /> Отмена
              </button>
            )}
          </div>
        </form>
      )}
      <div className="table-wrap wide">
        <table>
          <thead>
            <tr>
              <th>№</th>
              <th>Ресторан</th>
              <th>Фрукт</th>
              <th>Кол-во</th>
              <th>Поставщик</th>
              <th>Цена продажи</th>
              <th>Сумма</th>
              <th>Прибыль</th>
              <th>Даты</th>
              <th>Статус</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td>{order.id}</td>
                <td>{order.restaurant}</td>
                <td>{order.fruitName}</td>
                <td>{number(order.quantityKg)} кг</td>
                <td>{order.selectedSupplierName ?? '—'}</td>
                <td>{money(order.salePrice)}</td>
                <td>{money(order.orderAmount)}</td>
                <td>{money(order.profitAmount)}</td>
                <td>{order.orderDate} → {order.deliveryDate}</td>
                <td><span className={`status ${order.status === 'SUPPLIER_SELECTED' ? 'ok' : 'warn'}`}>{order.statusLabel ?? statusLabel(order.status)}</span></td>
                {canWrite && (
                  <td className="row-actions">
                    <button className="icon-button" title="Редактировать" onClick={() => {
                      setEditingId(order.id);
                      setForm({
                        restaurant: order.restaurant,
                        fruitName: order.fruitName,
                        quantityKg: order.quantityKg,
                        orderDate: order.orderDate,
                        deliveryDate: order.deliveryDate,
                      });
                    }}>
                      <Edit3 size={16} />
                    </button>
                    <button className="icon-button danger" title="Удалить" onClick={() => remove(order.id)}>
                      <Trash2 size={16} />
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function statusLabel(status) {
  return status === 'SUPPLIER_SELECTED' ? 'Подобран поставщик' : 'Требует ручного разбора';
}

function FinancePage({ finance, user }) {
  return (
    <section className="workspace">
      <div className="kpi-grid">
        <div className="kpi">
          <span>{can(user, 'finance:company') ? 'Общая выручка' : 'Моя выручка'}</span>
          <strong>{money(finance.totalRevenue)}</strong>
        </div>
        <div className="kpi accent">
          <span>{can(user, 'finance:company') ? 'Общая прибыль' : 'Моя прибыль'}</span>
          <strong>{money(finance.totalProfit)}</strong>
        </div>
      </div>
      <div className="charts-grid">
        <LineChart title="Выручка по дням" rows={finance.revenueByDay ?? []} />
        <BarChart title="Топ клиентов по прибыли" rows={(finance.profitByClient ?? []).slice(0, 6)} />
      </div>
      <div className="charts-grid">
        <BarChart title="Прибыль по фруктам" rows={finance.profitByFruit ?? []} />
        <BarChart title="Выручка по клиентам" rows={(finance.profitByClient ?? []).slice(0, 6)} valueKey="revenue" />
      </div>
      <div className="finance-grid">
        <FinanceTable title="Прибыль по фруктам" rows={finance.profitByFruit} />
        <FinanceTable title="Прибыль по клиентам" rows={finance.profitByClient} />
      </div>
    </section>
  );
}

function BarChart({ title, rows, valueKey = 'profit' }) {
  const max = Math.max(...rows.map((row) => Number(row[valueKey] ?? 0)), 1);
  return (
    <div className="chart-panel">
      <h2>{title}</h2>
      <div className="bar-chart">
        {rows.map((row) => {
          const value = Number(row[valueKey] ?? 0);
          return (
            <div className="bar-row" key={row.name}>
              <span title={row.name}>{row.name}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${Math.max((value / max) * 100, 3)}%` }} />
              </div>
              <strong>{money(value)}</strong>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function LineChart({ title, rows }) {
  const values = rows.map((row) => Number(row.revenue ?? 0));
  const max = Math.max(...values, 1);
  const points = rows.map((row, index) => {
    const x = rows.length <= 1 ? 50 : (index / (rows.length - 1)) * 100;
    const y = 100 - (Number(row.revenue ?? 0) / max) * 78 - 10;
    return { x, y, row };
  });
  const line = points.map((point) => `${point.x},${point.y}`).join(' ');

  return (
    <div className="chart-panel">
      <h2>{title}</h2>
      <svg className="line-chart" viewBox="0 0 100 100" preserveAspectRatio="none" role="img">
        <polyline points={line} fill="none" stroke="#2f7d62" strokeWidth="3" vectorEffect="non-scaling-stroke" />
        {points.map((point) => (
          <circle key={point.row.name} cx={point.x} cy={point.y} r="2.2" fill="#c77d2d" vectorEffect="non-scaling-stroke" />
        ))}
      </svg>
      <div className="line-labels">
        {rows.map((row) => (
          <span key={row.name}>{row.name.slice(5)}</span>
        ))}
      </div>
    </div>
  );
}

function FinanceTable({ title, rows }) {
  return (
    <div className="finance-table">
      <h2>{title}</h2>
      <table>
        <thead>
          <tr>
            <th>Название</th>
            <th>Выручка</th>
            <th>Прибыль</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.name}>
              <td>{row.name}</td>
              <td>{money(row.revenue)}</td>
              <td>{money(row.profit)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AuditPage({ logs }) {
  return (
    <section className="workspace">
      <div className="table-wrap wide">
        <table>
          <thead>
            <tr>
              <th>Время</th>
              <th>Пользователь</th>
              <th>Роль</th>
              <th>Действие</th>
              <th>Объект</th>
              <th>Описание</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{new Date(log.createdAt).toLocaleString('ru-RU')}</td>
                <td>{log.username}</td>
                <td>{log.roleLabel}</td>
                <td><span className={`status ${actionClass(log.action)}`}>{actionLabel(log.action)}</span></td>
                <td>{log.entityType} #{log.entityId}</td>
                <td className="details-cell">{log.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function actionLabel(action) {
  return {
    CREATE: 'Создание',
    UPDATE: 'Редактирование',
    DELETE: 'Удаление',
  }[action] ?? action;
}

function actionClass(action) {
  return {
    CREATE: 'ok',
    UPDATE: 'info',
    DELETE: 'danger-status',
  }[action] ?? 'info';
}

createRoot(document.getElementById('root')).render(<App />);
