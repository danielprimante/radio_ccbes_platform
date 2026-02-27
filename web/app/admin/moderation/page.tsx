'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getReports, updateReportStatus, deletePost, deleteComment, banUser, getUser, type Report } from '@/lib/api';
import Link from 'next/link';
import AdminGuard from '@/components/AdminGuard';

export default function ModerationPage() {
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<'pending' | 'reviewed' | 'dismissed'>('pending');

    useEffect(() => {
        loadReports();
    }, []);

    const loadReports = async () => {
        try {
            setLoading(true);
            const data = await getReports();
            setReports(data);
        } catch (error) {
            console.error('Error loading reports:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (reportId: string, action: 'dismiss' | 'delete' | 'ban', targetId?: string, type?: 'post' | 'comment', userId?: string) => {
        try {
            if (action === 'dismiss') {
                await updateReportStatus(reportId, 'dismissed');
            } else if (action === 'delete' && targetId) {
                if (type === 'post') {
                    await deletePost(targetId);
                } else {
                    await deleteComment(targetId);
                }
                await updateReportStatus(reportId, 'reviewed');
            } else if (action === 'ban' && userId) {
                if (confirm('¿Estás seguro de que deseas banear a este usuario?')) {
                    await banUser(userId);
                    await updateReportStatus(reportId, 'reviewed');
                }
            }
            await loadReports();
        } catch (error) {
            console.error('Error performing action:', error);
            alert('Error al realizar la acción');
        }
    };

    const filteredReports = reports.filter(r => r.status === filter);

    if (loading) {
        return (
            <div className="flex items-center justify-center p-20">
                <div className="text-gray-400 animate-pulse font-medium">Cargando reportes...</div>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-6 flex gap-4">
                {(['pending', 'reviewed', 'dismissed'] as const).map((s) => (
                    <button
                        key={s}
                        onClick={() => setFilter(s)}
                        className={`px-4 py-2 rounded-lg font-medium transition-colors ${filter === s
                            ? 'bg-red-600 text-white'
                            : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
                            }`}
                    >
                        {s === 'pending' ? 'Pendientes' : s === 'reviewed' ? 'Revisados' : 'Descartados'}
                    </button>
                ))}
            </div>

            <div className="bg-white rounded-lg shadow overflow-hidden overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 min-w-[800px]">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Motivo</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Reportado por</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Acciones</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {filteredReports.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                                    No hay reportes en esta categoría
                                </td>
                            </tr>
                        ) : (
                            filteredReports.map((report) => (
                                <tr key={report.id}>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${report.postId ? 'bg-blue-100 text-blue-800' : 'bg-purple-100 text-purple-800'
                                            }`}>
                                            {report.postId ? 'Publicación' : 'Comentario'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm text-gray-900">{report.reason}</div>
                                        {(report.postId || report.commentId) && (
                                            <div className="text-xs text-gray-500 mt-1">ID: {report.postId || report.commentId}</div>
                                        )}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {report.reportedBy}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {new Date(report.timestamp.seconds * 1000).toLocaleString()}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-2">
                                        {report.status === 'pending' && (
                                            <>
                                                <button
                                                    onClick={() => handleAction(report.id!, 'dismiss')}
                                                    className="text-gray-600 hover:text-gray-900 bg-gray-100 px-3 py-1 rounded"
                                                >
                                                    Descartar
                                                </button>
                                                <button
                                                    onClick={() => handleAction(
                                                        report.id!,
                                                        'delete',
                                                        report.postId || report.commentId,
                                                        report.postId ? 'post' : 'comment'
                                                    )}
                                                    className="text-red-600 hover:text-red-900 bg-red-50 px-3 py-1 rounded"
                                                >
                                                    Eliminar Contenido
                                                </button>
                                                <button
                                                    onClick={() => {
                                                        alert('ID de usuario no disponible en este reporte. Se requiere actualización del sistema de reportes.');
                                                    }}
                                                    className="text-orange-600 hover:text-orange-900 bg-orange-50 px-3 py-1 rounded"
                                                >
                                                    Banear Usuario
                                                </button>
                                            </>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
