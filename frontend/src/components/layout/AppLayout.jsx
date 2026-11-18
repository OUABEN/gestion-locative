import React, { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import styles from './AppLayout.module.css';

export const AppLayout = () => {
  // Sur desktop, le panneau latéral est ouvert par défaut et peut être ouvert/fermé à volonté
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const location = useLocation();

  const toggleSidebar = () => setSidebarOpen((prev) => !prev);

  return (
    <div className={`${styles.layout} ${sidebarOpen ? styles.withSidebar : styles.withoutSidebar}`}>
      <Sidebar
        isOpen={sidebarOpen}
        onToggle={toggleSidebar}
        onCloseMobile={() => {
          if (window.innerWidth <= 992) setSidebarOpen(false);
        }}
      />
      <div className={styles.mainWrapper}>
        <Topbar isSidebarOpen={sidebarOpen} onToggleSidebar={toggleSidebar} />
        <main className={styles.content}>
          <div key={location.pathname} className="page-transition">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
