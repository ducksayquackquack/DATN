const sql = require("mssql");
const cfg = { user:"sa", password:"123456", server:"localhost", port:1433, database:"DirtyWave", options:{encrypt:false,trustServerCertificate:true} };
(async()=>{
  const pool = await sql.connect(cfg);
  const r = await pool.request().query(`
    SELECT sp.maSanPham, sp.tenSanPham, ISNULL(ms.tenMau,'NULL/Empty') as tenMau, ms.id as mauSacId, ks.tenKichThuoc, spct.soLuong, spct.trangThai
    FROM SanPham sp
    INNER JOIN SanPhamChiTiet spct ON spct.idSanPham = sp.id
    LEFT JOIN MauSac ms ON ms.id = spct.idMauSac
    LEFT JOIN KichThuoc ks ON ks.id = spct.idKichThuoc
    WHERE sp.maSanPham IN ('SP013','SP023')
    ORDER BY sp.maSanPham, ms.tenMau, ks.tenKichThuoc
  `);
  console.log(JSON.stringify(r.recordset, null, 2));
  await pool.close();
})().catch(e=>{ console.error(e.message); process.exit(1); });
