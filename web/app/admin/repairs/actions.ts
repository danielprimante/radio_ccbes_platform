// This file is a stub to allow static export (output: 'export'). 
// Server Actions and firebase-admin are not supported in static builds.

export interface RepairStats {
  totalPosts: number;
  repairedPosts: number;
  errors: number;
  details: string[];
}

export async function repairAllLikesAction(): Promise<RepairStats> {
  throw new Error('La reparación masiva no está disponible en la versión estática (hosting plano). Debe usarse un servidor Node.js o realizarse manualmente en la consola de Firebase.');
}

export async function verifyPostLikesAction(postId: string) {
  throw new Error('La verificación no está disponible en la versión estática.');
}
