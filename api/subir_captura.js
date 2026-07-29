export default async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method === 'POST') {
        try {
            const body = req.body || {};
            const reportante = body.reportante || 'Patricio Diaz';
            const proyecto = body.proyecto || 'Carrera Pinto';
            const tipo = body.tipo || 'gasto';
            const fotoBase64 = body.fotoBase64 || '';
            const audioBase64 = body.audioBase64 || '';

            const timestamp = new Date().toISOString().replace(/[-:T.]/g, '').substring(0, 14);

            return res.status(200).json({
                status: 'success',
                message: `Captura procesada para ${reportante}`,
                timestamp: timestamp,
                proyecto: proyecto
            });
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    }

    return res.status(405).json({ error: 'Method not allowed' });
}
