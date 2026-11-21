export const particlesOptions = {
  fpsLimit: 120,
  fullScreen: {
    enable: true, // 关键：让粒子只在 .particles-bg 容器内
    zIndex: 0,
  },
  detectRetina: true,
  particles: {
    number: {
      value: 70,
      density: { enable: true, area: 1200 },
    },
    color: {
      value: ['#ffffff', '#d0e6ff', '#a0c8ff'],
    },
    shape: { type: 'circle' },
    opacity: {
      value: { min: 0.3, max: 0.8 },
      animation: { enable: true, speed: 2, minimumValue: 0.3 },
    },
    size: {
      value: { min: 2, max: 6 },
      animation: { enable: true, speed: 20, minimumValue: 2 },
    },
    links: {
      enable: true,
      distance: 150,
      color: '#ffffff',
      opacity: 0.35,
      width: 1.5,
    },
    move: {
      enable: true,
      speed: 1.8,
      direction: 'none',
      random: true,
      outModes: { default: 'out' },
    },
  },
  interactivity: {
    detectsOn: 'window',
    events: {
      onHover: { enable: true, mode: 'grab' },
      onClick: { enable: true, mode: 'push' },
      resize: true,
    },
    modes: {
      grab: { distance: 180, links: { opacity: 0.6 } },
      push: { quantity: 4 },
    },
  },
}