import { Component, computed, input } from '@angular/core';

/** Paleta fija; el color de cada avatar sale de una función determinística
 * del nombre (mismo nombre → mismo color siempre), no de datos que no existen. */
const PALETA = ['#3b82f6', '#7c3aed', '#0891b2', '#059669', '#d97706', '#db2777', '#4f46e5'];

/**
 * Avatar circular con las iniciales de un nombre (primera letra de las
 * primeras dos palabras). Puramente visual: no representa ninguna foto ni
 * dato que el backend no tenga.
 */
@Component({
  selector: 'app-avatar',
  template: `<span class="avatar" [style.background]="color()">{{ iniciales() }}</span>`,
})
export class Avatar {
  readonly nombre = input.required<string>();

  protected readonly iniciales = computed(() => {
    const palabras = this.nombre().trim().split(/\s+/).filter(Boolean);
    const primeras = palabras.slice(0, 2).map((palabra) => palabra[0]);
    return (primeras.join('') || '?').toUpperCase();
  });

  protected readonly color = computed(() => {
    const texto = this.nombre();
    let hash = 0;
    for (let i = 0; i < texto.length; i++) {
      hash = (hash * 31 + texto.charCodeAt(i)) >>> 0;
    }
    return PALETA[hash % PALETA.length];
  });
}
