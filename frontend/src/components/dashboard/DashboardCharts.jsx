import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  AreaChart,
  Area,
} from 'recharts';
import { Card } from '../ui/Card';
import { formatCurrency } from '../../utils/format';
import styles from './DashboardCharts.module.css';

const PALETTE = {
  lime: '#C6F432',
  limeDark: '#95BF11',
  midnight: '#0B0B0C',
  slate: '#52525E',
  muted: '#D8D9E2',
};

const TYPE_COLORS = ['#0B0B0C', '#C6F432', '#26262E', '#9EC818', '#4A4A56', '#7DA30B'];

// Infobulle Midnight Black & Lime Green
const ChartTooltip = ({ active, payload, label, currency }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className={styles.tooltip}>
      {label && <div className={styles.tooltipLabel}>{label}</div>}
      {payload.map((p) => (
        <div key={p.dataKey || p.name} className={styles.tooltipRow}>
          <span className={styles.tooltipDot} style={{ background: p.color || p.payload?.color }} />
          <span>{p.name}</span>
          <strong>{currency ? formatCurrency(p.value) : `${p.value} bien(s)`}</strong>
        </div>
      ))}
    </div>
  );
};

export const DashboardCharts = ({ biens }) => {
  const total = biens.length;
  const libres = biens.filter((b) => b.statut === 'LIBRE').length;
  const occupes = total - libres;
  const taux = total ? Math.round((occupes / total) * 100) : 0;

  const statusData = [
    { name: 'Occupé', value: occupes, color: PALETTE.midnight },
    { name: 'Libre', value: libres, color: PALETTE.lime },
  ];

  // Revenus par ville (réalisé vs potentiel)
  const villeMap = biens.reduce((acc, b) => {
    const v = b.ville || 'Autre';
    if (!acc[v]) acc[v] = { ville: v, potentiel: 0, realise: 0 };
    const loyer = Number(b.prixMensuel) || 0;
    acc[v].potentiel += loyer;
    if (b.statut === 'OCCUPE') acc[v].realise += loyer;
    return acc;
  }, {});
  const villeData = Object.values(villeMap).sort((a, b) => b.potentiel - a.potentiel);

  // Typologie
  const typeMap = biens.reduce((acc, b) => {
    const t = b.type || 'Autre';
    acc[t] = (acc[t] || 0) + 1;
    return acc;
  }, {});
  const typeData = Object.entries(typeMap)
    .map(([type, totalType], i) => ({ type, total: totalType, color: TYPE_COLORS[i % TYPE_COLORS.length] }))
    .sort((a, b) => b.total - a.total);

  return (
    <div className={styles.chartsGrid}>
      {/* Revenus par ville (Carte Midnight Black) */}
      <Card className={`${styles.chartCard} ${styles.darkChartCard} ${styles.wide} animate-fade-up`} style={{ animationDelay: '150ms' }}>
        <div className={styles.chartHeader}>
          <div>
            <h3 className={styles.chartTitleLight}>Revenus locatifs par ville</h3>
            <p className={styles.chartSubtitleLight}>Loyers réalisés (biens occupés) vs potentiel total</p>
          </div>
          <div className={styles.legendLight}>
            <span><i style={{ background: PALETTE.lime }} /> Réalisé</span>
            <span><i style={{ background: '#5E5E6E' }} /> Potentiel</span>
          </div>
        </div>
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={villeData} margin={{ top: 10, right: 12, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="gradRealise" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={PALETTE.lime} stopOpacity={0.55} />
                <stop offset="100%" stopColor={PALETTE.lime} stopOpacity={0.02} />
              </linearGradient>
              <linearGradient id="gradPotentiel" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#FFFFFF" stopOpacity={0.22} />
                <stop offset="100%" stopColor="#FFFFFF" stopOpacity={0.01} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="4 6" stroke="rgba(255,255,255,0.08)" vertical={false} />
            <XAxis dataKey="ville" tickLine={false} axisLine={false} tick={{ fill: '#A8A8B6', fontSize: 12 }} />
            <YAxis
              tickLine={false}
              axisLine={false}
              width={56}
              tick={{ fill: '#A8A8B6', fontSize: 11 }}
              tickFormatter={(v) => (v >= 1000 ? `${Math.round(v / 1000)}k` : v)}
            />
            <Tooltip content={<ChartTooltip currency />} cursor={{ stroke: '#C6F432', strokeDasharray: '4 4' }} />
            <Area
              type="monotone"
              dataKey="potentiel"
              name="Potentiel"
              stroke="#767686"
              strokeWidth={2}
              fill="url(#gradPotentiel)"
              animationDuration={1400}
            />
            <Area
              type="monotone"
              dataKey="realise"
              name="Réalisé"
              stroke={PALETTE.lime}
              strokeWidth={3}
              fill="url(#gradRealise)"
              activeDot={{ r: 6, strokeWidth: 3, stroke: '#0B0B0C', fill: '#C6F432' }}
              animationDuration={1600}
            />
          </AreaChart>
        </ResponsiveContainer>
      </Card>

      {/* Donut statut */}
      <Card className={`${styles.chartCard} animate-fade-up`} style={{ animationDelay: '250ms' }}>
        <div className={styles.chartHeader}>
          <div>
            <h3 className={styles.chartTitle}>Occupation du parc</h3>
            <p className={styles.chartSubtitle}>Répartition LIBRE / OCCUPÉ</p>
          </div>
        </div>
        <div className={styles.donutWrapper}>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={statusData}
                cx="50%"
                cy="50%"
                innerRadius={68}
                outerRadius={92}
                paddingAngle={4}
                cornerRadius={8}
                dataKey="value"
                stroke="none"
                startAngle={90}
                endAngle={-270}
                animationDuration={1300}
              >
                {statusData.map((entry) => (
                  <Cell key={entry.name} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip content={<ChartTooltip />} />
            </PieChart>
          </ResponsiveContainer>
          <div className={styles.donutCenter}>
            <span className={styles.donutValue}>{taux}%</span>
            <span className={styles.donutLabel}>occupés</span>
          </div>
        </div>
        <div className={styles.statusLegend}>
          {statusData.map((s) => (
            <div key={s.name} className={styles.statusItem}>
              <i style={{ background: s.color }} />
              <span>{s.name}</span>
              <strong>{s.value}</strong>
            </div>
          ))}
        </div>
      </Card>

      {/* Typologie */}
      <Card className={`${styles.chartCard} ${styles.wideHalf} animate-fade-up`} style={{ animationDelay: '350ms' }}>
        <div className={styles.chartHeader}>
          <div>
            <h3 className={styles.chartTitle}>Typologie des biens</h3>
            <p className={styles.chartSubtitle}>Nombre de biens par catégorie</p>
          </div>
        </div>
        <ResponsiveContainer width="100%" height={Math.max(180, typeData.length * 46)}>
          <BarChart data={typeData} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 4 }}>
            <XAxis type="number" hide allowDecimals={false} />
            <YAxis
              dataKey="type"
              type="category"
              width={130}
              tickLine={false}
              axisLine={false}
              tick={{ fill: '#0B0B0C', fontSize: 13, fontWeight: 600 }}
            />
            <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(198, 244, 50, 0.14)' }} />
            <Bar
              dataKey="total"
              name="Biens"
              radius={[0, 10, 10, 0]}
              barSize={22}
              background={{ fill: '#F0F1F5', radius: 10 }}
              animationDuration={1300}
              label={{ position: 'right', fill: '#0B0B0C', fontSize: 12, fontWeight: 700 }}
            >
              {typeData.map((entry) => (
                <Cell key={entry.type} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </Card>
    </div>
  );
};
