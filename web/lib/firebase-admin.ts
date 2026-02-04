import { initializeApp, getApps, cert, getApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';

const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT_KEY
    ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_KEY)
    : undefined;

// Basic configuration check to avoid crashes in build/dev if env vars are missing
// In a real production environment, you must ensure these are present.
const firebaseAdminConfig = {
    credential: serviceAccount ? cert(serviceAccount) : undefined,
    projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
};

function customInitApp() {
    if (getApps().length <= 0) {
        // If we don't have a service account (e.g. during build or local dev without keys), 
        // initializeApp might fail or warn if we try to use cert().
        // For now, we attempt to initialize. If serviceAccount is undefined, it might try default credentials.
        return initializeApp(firebaseAdminConfig);
    }
    return getApp();
}

const app = customInitApp();
export const adminAuth = getAuth(app);
export const adminDb = getFirestore(app);
