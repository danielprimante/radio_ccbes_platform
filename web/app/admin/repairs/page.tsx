'use client';

import { useState } from 'react';
import { repairAllLikesAction, verifyPostLikesAction, RepairStats } from './actions';

export default function RepairsPage() {
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState<RepairStats | null>(null);
    const [verifyId, setVerifyId] = useState('');
    const [verifyResult, setVerifyResult] = useState<any>(null);

    const handleRepairAll = async () => {
        if (!confirm('¿Estás seguro de que quieres recalcular los likes de TODOS los posts? Esta operación puede tardar.')) return;

        setLoading(true);
        setStats(null);
        try {
            const result = await repairAllLikesAction();
            setStats(result);
        } catch (error) {
            alert('Error ejecutando la reparación: ' + error);
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async () => {
        if (!verifyId) return;
        setLoading(true);
        setVerifyResult(null);
        try {
            const result = await verifyPostLikesAction(verifyId);
            setVerifyResult(result);
        } catch (error) {
            alert('Error: ' + error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-8 text-gray-800">🛠️ Reparación de Datos</h1>

            <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-8">
                <div className="flex">
                    <div className="ml-3">
                        <p className="text-sm text-yellow-700">
                            <strong>Nota:</strong> Estas herramientas administrativas requieren un servidor activo y no funcionan en el modo de "Exportación Estática" (Hosting básico).
                        </p>
                    </div>
                </div>
            </div>

            {/* Global Repair Section */}
            <div className="bg-white rounded-lg shadow-md p-6 mb-8 border border-gray-100">
                <h2 className="text-xl font-semibold mb-4 text-blue-800">Sincronización Global de Likes</h2>
                <p className="text-gray-600 mb-6">
                    Esta herramienta audita todos los posts y recalcula el contador de likes basándose en los documentos reales existentes en la colección `likes`.
                </p>

                <button
                    onClick={handleRepairAll}
                    disabled={loading}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-lg transition-colors disabled:opacity-50 flex items-center gap-2"
                >
                    {loading ? 'Procesando...' : 'Ejecutar Reparación Masiva'}
                </button>

                {stats && (
                    <div className="mt-6 p-4 bg-gray-50 rounded-lg border border-gray-200 animate-in fade-in slide-in-from-top-4">
                        <h3 className="font-bold text-lg mb-2 text-green-700">✅ Resultado:</h3>
                        <ul className="space-y-1 text-sm">
                            <li>Total Posts Escaneados: <strong>{stats.totalPosts}</strong></li>
                            <li>Posts Reparados: <strong>{stats.repairedPosts}</strong></li>
                            <li>Errores: <strong>{stats.errors}</strong></li>
                        </ul>
                        {stats.details.length > 0 && (
                            <div className="mt-4">
                                <p className="font-semibold text-xs text-gray-500 uppercase tracking-wide mb-2">Detalles:</p>
                                <div className="bg-gray-800 text-green-400 p-3 rounded text-xs font-mono h-40 overflow-y-auto">
                                    {stats.details.map((line, i) => (
                                        <div key={i}>{line}</div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Verify Single Post Section */}
            <div className="bg-white rounded-lg shadow-md p-6 border border-gray-100">
                <h2 className="text-xl font-semibold mb-4 text-purple-800">Verificar Post Individual</h2>
                <div className="flex gap-4 mb-4">
                    <input
                        type="text"
                        placeholder="ID del Post"
                        value={verifyId}
                        onChange={(e) => setVerifyId(e.target.value)}
                        className="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-purple-500 outline-none"
                    />
                    <button
                        onClick={handleVerify}
                        disabled={loading || !verifyId}
                        className="bg-purple-600 hover:bg-purple-700 text-white font-bold py-2 px-6 rounded-lg transition-colors disabled:opacity-50"
                    >
                        Verificar
                    </button>
                </div>

                {verifyResult && (
                    <div className="mt-4 p-4 bg-purple-50 rounded-lg border border-purple-100">
                        <pre className="text-sm font-mono text-gray-700 whitespace-pre-wrap">
                            {JSON.stringify(verifyResult, null, 2)}
                        </pre>
                    </div>
                )}
            </div>
        </div>
    );
}
