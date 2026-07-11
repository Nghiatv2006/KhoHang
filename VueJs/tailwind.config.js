/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class', '.dark-mode'],
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#00288e", // Corporate Blue
          hover: "#173bab",
          container: "#1e40af",
          light: "#dde1ff",
        },
        background: "#f7f9fb",
        surface: {
          DEFAULT: "#ffffff",
          dim: "#d8dadc",
          bright: "#f7f9fb",
          "container-low": "#f2f4f6",
          "container": "#eceef0",
          "container-high": "#e6e8ea",
          "container-highest": "#e0e3e5",
          lowest: "#ffffff",
        },
        "on-surface": "#191c1e",
        "on-surface-variant": "#444653",
        outline: "#757684",
        "outline-variant": "#c4c5d5",
        success: {
          DEFAULT: "#10b981", // Emerald Green
          light: "#d1fae5",
          dark: "#065f46",
        },
        warning: {
          DEFAULT: "#f59e0b", // Amber Orange
          light: "#fef3c7",
          dark: "#78350f",
        },
        error: {
          DEFAULT: "#ba1a1a", // Rose Red
          container: "#ffdad6",
          text: "#93000a",
        }
      },
      borderRadius: {
        sm: "0.125rem", // 2px
        DEFAULT: "0.25rem", // 4px
        md: "0.25rem", // 4px
        lg: "0.5rem", // 8px
        xl: "0.75rem", // 12px
      },
      spacing: {
        base: "4px",
        xs: "4px",
        sm: "8px",
        md: "16px",
        lg: "24px",
        xl: "32px",
        "container-margin": "24px",
        gutter: "16px",
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
      }
    },
  },
  plugins: [],
}
