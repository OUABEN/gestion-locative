import React from 'react';
import styles from './Button.module.css';

export const Button = ({
  children,
  variant = 'primary', // 'primary' | 'secondary' | 'danger' | 'ghost'
  size = 'md',        // 'sm' | 'md' | 'lg'
  disabled = false,
  onClick,
  type = 'button',
  icon: Icon,
  className = '',
  ...props
}) => {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`${styles.button} ${styles[variant]} ${styles[size]} ${className}`}
      {...props}
    >
      {Icon && <Icon size={size === 'sm' ? 16 : 18} className={styles.icon} />}
      <span>{children}</span>
    </button>
  );
};

